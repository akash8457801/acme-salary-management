package co.acme.salary.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.acme.salary.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * The aggregate maths, checked against a tiny org where the right answers are computable by hand.
 * Four Germans at 60/70/80/90k EUR and one American at 200k USD: the medians, totals and gaps
 * below are all pen-and-paper numbers, not snapshots of whatever the code produced.
 */
class InsightsApiTest extends IntegrationTestBase {

    private static final String PERSON = """
            {
              "firstName": "%s", "lastName": "%s", "email": "%s@acme.co",
              "departmentId": %d, "countryCode": "%s", "jobTitle": "Engineer",
              "level": "%s", "gender": "%s", "hireDate": "2023-01-09",
              "startingSalary": %d
            }""";

    private void hire(long departmentId, String first, String last, String country,
                      String level, String gender, int salary) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PERSON.formatted(first, last, first + "." + last,
                                departmentId, country, level, gender, salary)))
                .andExpect(status().isCreated());
    }

    private void hireHandCheckableOrg() throws Exception {
        long engineering = givenEngineeringInGermanyAndUs();
        // EUR converts at the shipped 1.0850: 65100, 75950, 86800, 97650 USD.
        hire(engineering, "Anna", "Fischer", "DE", "L2", "FEMALE", 60_000);
        hire(engineering, "Ben", "Weber", "DE", "L2", "MALE", 70_000);
        hire(engineering, "Clara", "Schmidt", "DE", "L3", "FEMALE", 80_000);
        hire(engineering, "David", "Braun", "DE", "L3", "MALE", 90_000);
        hire(engineering, "Erin", "Smith", "US", "L5", "FEMALE", 200_000);
    }

    @Test
    void overviewSumsAndMediansTheWholeOrgInUsd() throws Exception {
        hireHandCheckableOrg();

        mockMvc.perform(get("/api/insights/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.headcount").value(5))
                // 65100 + 75950 + 86800 + 97650 + 200000
                .andExpect(jsonPath("$.overview.totalAnnualUsd").value(525500))
                .andExpect(jsonPath("$.overview.averageAnnualUsd").value(105100))
                // odd count -> the middle value, 86800
                .andExpect(jsonPath("$.overview.medianAnnualUsd").value(86800))
                .andExpect(jsonPath("$.overview.countryCount").value(2));
    }

    @Test
    void countryBreakdownRanksByTotalCost() throws Exception {
        hireHandCheckableOrg();

        mockMvc.perform(get("/api/insights/dashboard"))
                // Germany: 4 people, 325,500 USD total — more than the single 200k American.
                .andExpect(jsonPath("$.byCountry[0].key").value("DE"))
                .andExpect(jsonPath("$.byCountry[0].headcount").value(4))
                .andExpect(jsonPath("$.byCountry[0].totalAnnualUsd").value(325500))
                // even count -> mean of the two middle values, (75950 + 86800) / 2
                .andExpect(jsonPath("$.byCountry[0].medianAnnualUsd").value(81375))
                .andExpect(jsonPath("$.byCountry[1].key").value("US"));
    }

    @Test
    void payEquityComparesMediansWithinALevel() throws Exception {
        hireHandCheckableOrg();

        mockMvc.perform(get("/api/insights/dashboard"))
                // L2: Anna 65100 vs Ben 75950 -> gap (75950-65100)/75950 = 14.3%
                .andExpect(jsonPath("$.payEquity[0].level").value("L2"))
                .andExpect(jsonPath("$.payEquity[0].gapPercent").value(14.3))
                // L5 has only Erin: no male group, so no gap is asserted rather than zero.
                .andExpect(jsonPath("$.payEquity[2].level").value("L5"))
                .andExpect(jsonPath("$.payEquity[2].gapPercent").doesNotExist());
    }

    @Test
    void distributionBucketsPeopleIntoUsdBands() throws Exception {
        hireHandCheckableOrg();

        mockMvc.perform(get("/api/insights/dashboard"))
                // 65100 and 75950 fall in the 50-75k and 75-100k bands respectively.
                .andExpect(jsonPath("$.distribution[0].lowerBoundUsd").value(50000))
                .andExpect(jsonPath("$.distribution[0].headcount").value(1))
                .andExpect(jsonPath("$.distribution[1].lowerBoundUsd").value(75000))
                .andExpect(jsonPath("$.distribution[1].headcount").value(3))
                .andExpect(jsonPath("$.distribution[2].lowerBoundUsd").value(200000))
                .andExpect(jsonPath("$.distribution[2].headcount").value(1));
    }

    @Test
    void terminatedEmployeesDropOutOfEveryNumber() throws Exception {
        hireHandCheckableOrg();
        jdbc.update("update employee set status = 'TERMINATED' where last_name = 'Smith'");

        mockMvc.perform(get("/api/insights/dashboard"))
                .andExpect(jsonPath("$.overview.headcount").value(4))
                .andExpect(jsonPath("$.overview.totalAnnualUsd").value(325500))
                .andExpect(jsonPath("$.overview.countryCount").value(1));
    }
}
