package co.acme.salary.repository;

import co.acme.salary.query.BreakdownDimension;
import co.acme.salary.query.InsightModels.BreakdownRow;
import co.acme.salary.query.InsightModels.OrgOverview;
import co.acme.salary.query.InsightModels.SalaryBand;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Aggregate queries behind the insights dashboard.
 *
 * <p>Written as SQL against {@link JdbcTemplate} rather than JPA on purpose. These are reporting
 * questions — "what does Engineering cost", "what is the median in Germany" — and they are
 * answered by grouping and window functions, not by loading objects. Every one of them runs over
 * the whole 10,000-employee dataset without materialising a single entity.
 *
 * <p>Only the <em>open</em> compensation record counts, and terminated employees are excluded:
 * payroll cost means what we are paying now.
 */
@Repository
public class InsightsRepository {

    /** Width of one histogram band, in USD, and the number of bands before the open-ended top one. */
    private static final int BAND_WIDTH_USD = 25_000;
    private static final int BAND_COUNT = 12;

    private static final String CURRENTLY_PAID = """
            from employee e
            join country c on c.code = e.country_code
            join department d on d.id = e.department_id
            join compensation_record cr
              on cr.employee_id = e.id and cr.effective_to is null
            where e.status <> 'TERMINATED'
            """;

    private final JdbcTemplate jdbc;

    public InsightsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OrgOverview overview() {
        String sql = """
                with paid as (
                  select cr.annual_usd_amount as amt, e.country_code as cc, e.department_id as did
                  %s
                ),
                ranked as (
                  select amt, row_number() over (order by amt) as rn, count(*) over () as cnt from paid
                )
                select
                  (select count(*) from paid)                    as headcount,
                  (select coalesce(sum(amt), 0) from paid)       as total,
                  (select coalesce(avg(amt), 0) from paid)       as average,
                  (select coalesce(avg(amt), 0) from ranked
                     where rn in ((cnt + 1) / 2, (cnt + 2) / 2))  as median,
                  (select count(distinct cc) from paid)          as countries,
                  (select count(distinct did) from paid)         as departments
                """.formatted(CURRENTLY_PAID);

        return jdbc.queryForObject(sql, (rs, rowNum) -> new OrgOverview(
                rs.getLong("headcount"),
                scaled(rs.getBigDecimal("total")),
                scaled(rs.getBigDecimal("average")),
                scaled(rs.getBigDecimal("median")),
                rs.getInt("countries"),
                rs.getInt("departments")));
    }

    /**
     * Headcount, cost and salary spread per group.
     *
     * <p>Medians and quartiles use the nearest-rank method over a windowed ordering, which both
     * SQLite and Postgres support. {@code PERCENTILE_CONT} would be tidier but ties the reporting
     * layer to one database.
     */
    public List<BreakdownRow> breakdownBy(BreakdownDimension dimension) {
        String sql = """
                with paid as (
                  select %s as k, %s as label, cr.annual_usd_amount as amt
                  %s
                ),
                ranked as (
                  select k, amt,
                         row_number() over (partition by k order by amt) as rn
                  from paid
                ),
                grouped as (
                  select k, label, count(*) as headcount, sum(amt) as total, avg(amt) as average
                  from paid group by k, label
                )
                select g.k, g.label, g.headcount, g.total, g.average,
                  (select avg(amt) from ranked r where r.k = g.k
                     and r.rn in ((g.headcount + 1) / 2, (g.headcount + 2) / 2))            as median,
                  (select amt from ranked r where r.k = g.k
                     and r.rn = max(1, (g.headcount * 25 + 99) / 100))                      as p25,
                  (select amt from ranked r where r.k = g.k
                     and r.rn = max(1, (g.headcount * 75 + 99) / 100))                      as p75
                from grouped g
                order by g.total desc
                """.formatted(dimension.keyExpression(), dimension.labelExpression(), CURRENTLY_PAID);

        return jdbc.query(sql, breakdownRowMapper());
    }

    /** Salary distribution in fixed USD bands — the shape of how ACME pays, not just the average. */
    public List<SalaryBand> distribution() {
        String sql = """
                with paid as (
                  select cr.annual_usd_amount as amt
                  %s
                )
                select min(cast(amt / %d as integer), %d) as band, count(*) as headcount
                from paid
                group by band
                order by band
                """.formatted(CURRENTLY_PAID, BAND_WIDTH_USD, BAND_COUNT);

        return jdbc.query(sql, (rs, rowNum) -> {
            int band = rs.getInt("band");
            BigDecimal lower = BigDecimal.valueOf((long) band * BAND_WIDTH_USD);
            BigDecimal upper = band == BAND_COUNT
                    ? null
                    : BigDecimal.valueOf((long) (band + 1) * BAND_WIDTH_USD);
            return new SalaryBand(lower, upper, rs.getLong("headcount"));
        });
    }

    /** Headcount and median pay for every (level, gender) pair that has anyone in it. */
    public List<GenderMedian> medianByLevelAndGender() {
        String sql = """
                with paid as (
                  select e.job_level as lvl, e.gender as gender, cr.annual_usd_amount as amt
                  %s
                ),
                ranked as (
                  select lvl, gender, amt,
                         row_number() over (partition by lvl, gender order by amt) as rn
                  from paid
                ),
                grouped as (
                  select lvl, gender, count(*) as headcount from paid group by lvl, gender
                )
                select g.lvl, g.gender, g.headcount,
                  (select avg(amt) from ranked r
                     where r.lvl = g.lvl and r.gender = g.gender
                     and r.rn in ((g.headcount + 1) / 2, (g.headcount + 2) / 2)) as median
                from grouped g
                order by g.lvl, g.gender
                """.formatted(CURRENTLY_PAID);

        return jdbc.query(sql, (rs, rowNum) -> new GenderMedian(
                rs.getString("lvl"),
                rs.getString("gender"),
                rs.getLong("headcount"),
                scaled(rs.getBigDecimal("median"))));
    }

    private RowMapper<BreakdownRow> breakdownRowMapper() {
        return (rs, rowNum) -> new BreakdownRow(
                rs.getString("k"),
                rs.getString("label"),
                rs.getLong("headcount"),
                scaled(rs.getBigDecimal("total")),
                scaled(rs.getBigDecimal("average")),
                scaled(rs.getBigDecimal("median")),
                scaled(rs.getBigDecimal("p25")),
                scaled(rs.getBigDecimal("p75")));
    }

    /** SQLite hands back averages as floating point; round once, here, to whole dollars. */
    private static BigDecimal scaled(BigDecimal value) {
        return value == null ? null : value.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    /** Intermediate row for the pay-equity report; assembled into level rows by the service. */
    public record GenderMedian(String level, String gender, long headcount, BigDecimal medianUsd) {
    }
}
