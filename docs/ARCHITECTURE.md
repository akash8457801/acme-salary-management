# Architecture

## Shape

```
Angular 19 SPA  ──HTTP/JSON──►  Spring Boot 3 API  ──JPA──►  SQLite
(Angular Material)               (Java 17)                   (single file)
```

Two deployables, one repository. The SPA is built to static assets and served by the Spring Boot
app in the packaged jar, so production is a single `java -jar` process with no reverse proxy to
configure. In development the two run separately with a dev-server proxy.

## Backend layering

```
web/         REST controllers, request/response DTOs, validation, error handling
             ▲ never leaks JPA entities
service/     use cases — CompensationService, EmployeeQueryService, InsightsService
             ▲ owns transactions and domain invariants
domain/      entities + pure domain types (Money, FxRate, SalaryBand, timeline rules)
repository/  Spring Data JPA repositories + hand-written aggregate queries
seed/        deterministic data generator, runs once on an empty database
```

The rule that keeps this honest: **DTOs never appear below `web/`, entities never appear above
`service/`.** Controllers are thin enough to read in one screen.

## Domain model

```
Department 1───* Employee *───1 Country
                   │
                   │ 1
                   ▼ *
            CompensationRecord   (append-only timeline)
```

`Employee` holds identity and org placement. It deliberately has **no `salary` column** — current
pay is derived from the compensation timeline. A mutable salary field is exactly the spreadsheet
failure mode this product exists to fix.

`CompensationRecord` is the heart of the model:

| field | why |
|---|---|
| `amount` + `currency` | pay is always a Money, never a bare number |
| `effectiveFrom` / `effectiveTo` | closed-open interval; `effectiveTo = null` means "current" |
| `changeReason` | HIRE, MERIT_INCREASE, PROMOTION, MARKET_ADJUSTMENT, ROLE_CHANGE, CORRECTION |
| `annualUsdAmount` | denormalised at write time — see ADR-004 |
| `recordedAt` | when we learned it, as distinct from when it takes effect |

Invariants enforced in `CompensationService`, tested without a database:
- at most one open record per employee;
- intervals never overlap and never leave a gap;
- a new record's `effectiveFrom` must be after the current record's — you cannot silently rewrite
  history, you can only supersede it;
- amount must be positive and in a supported currency.

## Query strategy

10,000 employees is small for a database and large for a browser. Everything is therefore pushed
into SQL:

- **List:** one paged query with a dynamic `Specification` built from the filter DTO, joined to the
  open compensation record. Page size is capped server-side.
- **Insights:** hand-written aggregate queries returning projection interfaces — `GROUP BY` for
  headcount/sum/avg, and a windowed query for medians and percentiles. No entity ever loads.
- **CSV export:** streamed, not materialised in memory.

Indexes cover the filter columns (`department_id`, `country_code`, `level`, `status`) and the hot
timeline lookup (`employee_id, effective_to`).

## Frontend

Standalone Angular components, signal-based state, Angular Material for the component library.
Three feature areas — `employees` (list + detail), `insights` (dashboard), `shared` (money
formatting, filter state, API client). Filter and pagination state lives in the URL so a filtered
view is a shareable link, and the list re-queries the server on every change.

Charts are hand-rolled inline SVG rather than a charting dependency: the dashboard needs three
shapes (bar, histogram, grouped bar), and a small purpose-built component is less code and less
risk than configuring a general-purpose library.

## Testing

| layer | tool | what it proves |
|---|---|---|
| domain | plain JUnit 5, no Spring | timeline invariants, Money/FX arithmetic, banding, percentile maths |
| service | JUnit + Mockito | use-case orchestration and error paths |
| web/repo | `@SpringBootTest` + MockMvc on in-memory SQLite | wiring, serialisation, real SQL, filter correctness |
| frontend | Jasmine/Karma headless | list filtering, raise dialog validation, money pipe |

Every test is deterministic — fixed clock, fixed FX table, fixed seed. No test touches the network
or sleeps.
