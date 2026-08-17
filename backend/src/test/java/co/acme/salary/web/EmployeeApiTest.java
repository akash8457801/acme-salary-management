package co.acme.salary.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.acme.salary.testsupport.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * The API as the UI experiences it: JSON in, JSON out, real SQL underneath.
 */
class EmployeeApiTest extends IntegrationTestBase {

    private static final String ADA = """
            {
              "firstName": "Ada", "lastName": "Lovelace", "email": "ada@acme.co",
              "departmentId": %d, "countryCode": "DE", "jobTitle": "Senior Engineer",
              "level": "L3", "gender": "FEMALE", "hireDate": "2024-02-01",
              "startingSalary": 80000
            }""";

    private long hireAda() throws Exception {
        long departmentId = givenEngineeringInGermanyAndUs();
        String body = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADA.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Nested
    @DisplayName("hiring")
    class Hiring {

        @Test
        void createsThePersonAndTheirOpeningSalaryTogether() throws Exception {
            long id = hireAda();

            mockMvc.perform(get("/api/employees/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeCode").value("ACME-00001"))
                    .andExpect(jsonPath("$.payrollCurrency").value("EUR"))
                    .andExpect(jsonPath("$.currentCompensation.salary.amount").value(80000))
                    .andExpect(jsonPath("$.currentCompensation.reason").value("HIRE"))
                    // 80,000 EUR at the shipped 1.0850 rate
                    .andExpect(jsonPath("$.currentCompensation.annualUsd.amount").value(86800.0));
        }

        @Test
        void rejectsADuplicateEmailWith409() throws Exception {
            hireAda();

            mockMvc.perform(post("/api/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ADA.formatted(jdbc.queryForObject(
                                    "select id from department", Long.class))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("already in use")));
        }

        @Test
        void rejectsAMissingSalaryWithFieldLevelErrors() throws Exception {
            long departmentId = givenEngineeringInGermanyAndUs();

            mockMvc.perform(post("/api/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "No", "lastName": "Salary", "email": "no@acme.co",
                                     "departmentId": %d, "countryCode": "DE", "jobTitle": "Engineer",
                                     "level": "L2", "gender": "MALE", "hireDate": "2024-02-01"}
                                    """.formatted(departmentId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.startingSalary").exists());
        }
    }

    @Nested
    @DisplayName("recording a pay change")
    class PayChanges {

        @Test
        void supersedesTheCurrentSalaryAndKeepsBothPeriods() throws Exception {
            long id = hireAda();

            mockMvc.perform(post("/api/employees/" + id + "/compensation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"amount": 95000, "currency": "EUR", "effectiveFrom": "2025-07-01",
                                     "reason": "PROMOTION", "note": "Promoted to Staff"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.changePercent").value(18.75))
                    .andExpect(jsonPath("$.current").value(true));

            mockMvc.perform(get("/api/employees/" + id + "/compensation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].effectiveTo").value("2025-07-01"))
                    .andExpect(jsonPath("$[0].current").value(false))
                    .andExpect(jsonPath("$[1].current").value(true));
        }

        @Test
        void refusesToRewriteHistoryWith422() throws Exception {
            long id = hireAda();

            mockMvc.perform(post("/api/employees/" + id + "/compensation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"amount": 90000, "currency": "EUR", "effectiveFrom": "2023-01-01",
                                     "reason": "MERIT_INCREASE"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("hire date")));
        }

        @Test
        void refusesTheWrongCurrencyWith422() throws Exception {
            long id = hireAda();

            mockMvc.perform(post("/api/employees/" + id + "/compensation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"amount": 95000, "currency": "USD", "effectiveFrom": "2025-07-01",
                                     "reason": "PROMOTION"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("paid in EUR")));
        }

        @Test
        void isNotFoundForAnUnknownEmployee() throws Exception {
            mockMvc.perform(post("/api/employees/999/compensation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"amount": 95000, "currency": "EUR", "effectiveFrom": "2025-07-01",
                                     "reason": "PROMOTION"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("the directory")
    class Directory {

        @Test
        void findsPeopleByNameFragment() throws Exception {
            hireAda();

            mockMvc.perform(get("/api/employees").param("q", "lovel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].fullName").value("Ada Lovelace"));

            mockMvc.perform(get("/api/employees").param("q", "nobody"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void filtersBySalaryBandInUsd() throws Exception {
            hireAda(); // 86,800 USD annualised

            mockMvc.perform(get("/api/employees").param("minAnnualUsd", "90000"))
                    .andExpect(jsonPath("$.totalElements").value(0));

            mockMvc.perform(get("/api/employees")
                            .param("minAnnualUsd", "80000").param("maxAnnualUsd", "90000"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void capsThePageSizeInsteadOfTrustingTheClient() throws Exception {
            hireAda();

            mockMvc.perform(get("/api/employees").param("size", "10000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(200));
        }

        @Test
        void exportsTheFilteredViewAsCsv() throws Exception {
            hireAda();

            // StreamingResponseBody writes on an async dispatch, which MockMvc must be told to run.
            var asyncResult = mockMvc.perform(get("/api/employees/export").param("q", "ada"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .request().asyncStarted())
                    .andReturn();

            String csv = mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            org.assertj.core.api.Assertions.assertThat(csv)
                    .contains("Employee code,Name,Email")
                    .contains("ACME-00001,Ada Lovelace,ada@acme.co");
        }
    }

    @Nested
    @DisplayName("editing a profile")
    class Editing {

        @Test
        void updatesTheProfileWithoutTouchingCompensation() throws Exception {
            long id = hireAda();
            long departmentId = jdbc.queryForObject("select id from department", Long.class);

            mockMvc.perform(put("/api/employees/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Ada", "lastName": "King", "email": "ada@acme.co",
                                     "departmentId": %d, "jobTitle": "Staff Engineer", "level": "L4",
                                     "status": "ACTIVE"}
                                    """.formatted(departmentId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Ada King"))
                    .andExpect(jsonPath("$.level").value("L4"))
                    .andExpect(jsonPath("$.currentCompensation.salary.amount").value(80000));
        }

        @Test
        void refusesMakingSomeoneTheirOwnManager() throws Exception {
            long id = hireAda();
            long departmentId = jdbc.queryForObject("select id from department", Long.class);

            mockMvc.perform(put("/api/employees/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Ada", "lastName": "Lovelace", "email": "ada@acme.co",
                                     "departmentId": %d, "jobTitle": "Engineer", "level": "L3",
                                     "managerId": %d, "status": "ACTIVE"}
                                    """.formatted(departmentId, id)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("own manager")));
        }
    }
}
