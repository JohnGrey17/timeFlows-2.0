package example.timeflows.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:securitytests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
            "timeflows.demo-data.enabled=false"
        })
@AutoConfigureMockMvc
@Import(SecurityTestData.class)
class SecurityAuthorizationTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void loginPageContainsCsrfToken() throws Exception {
        mockMvc.perform(get("/api/login"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void privilegedLoginRequiresMfaEnrollmentBeforeIssuingFullSession() throws Exception {
        mockMvc.perform(
                        post("/api/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("email", "it.manager@vyriy.com")
                                .param("password", "test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .redirectedUrl("/api/mfa/setup"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                                .exists("TIMEFLOWS_MFA_PENDING"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                                .doesNotExist("TIMEFLOWS_JWT"));
    }

    @Test
    void mfaPageWithoutPendingSessionReturnsToLogin() throws Exception {
        mockMvc.perform(get("/api/mfa/challenge"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .redirectedUrl("/api/login?mfaExpired"));
    }

    @Test
    void registrationContinuesDirectlyToMandatoryMfaEnrollment() throws Exception {
        mockMvc.perform(
                        post("/api/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("firstName", "New")
                                .param("lastName", "Employee")
                                .param("email", "new.employee@vyriy.com")
                                .param("password", "test-password")
                                .param("divisionId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .redirectedUrl("/api/mfa/setup"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                                .exists("TIMEFLOWS_MFA_PENDING"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                                .doesNotExist("TIMEFLOWS_JWT"));
    }

    @Test
    void stateChangingRequestWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/departments")
                                .with(user("admin@vyriy.com").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Без CSRF\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void spaRequestAcceptsRawCookieTokenInHeader() throws Exception {
        MvcResult page = mockMvc.perform(get("/api/login")).andExpect(status().isOk()).andReturn();
        jakarta.servlet.http.Cookie csrfCookie = page.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(
                        post("/api/departments")
                                .with(user("admin@vyriy.com").roles("ADMIN"))
                                .cookie(csrfCookie)
                                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void employeeCannotCreateDepartmentEvenWithCsrf() throws Exception {
        mockMvc.perform(
                        post("/api/departments")
                                .with(user("employee@vyriy.com").roles("EMPLOYEE"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Заборонено\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPassesCsrfAndRoleChecksForDepartmentMutation() throws Exception {
        mockMvc.perform(
                        post("/api/departments")
                                .with(user("admin@vyriy.com").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerCanReadDepartments() throws Exception {
        mockMvc.perform(get("/api/departments").with(user("manager@vyriy.com").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotReadUsersRestApi() throws Exception {
        mockMvc.perform(get("/api/users/rest").with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenOrganizationManagement() throws Exception {
        mockMvc.perform(get("/api/organization").with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Керування відділами")));
    }

    @Test
    void employeeCannotOpenOrganizationManagement() throws Exception {
        mockMvc.perform(get("/api/organization").with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void excelExportPageIsAvailableOnlyToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/export").with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/admin/export")
                                .with(user("it.manager@vyriy.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/export").with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Експорт Excel")));
    }

    @Test
    void registrationContainsDepartmentAndDivisionSelectors() throws Exception {
        mockMvc.perform(get("/api/register"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "data-organization-department")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "data-organization-division")));
    }

    @Test
    void adminCanFilterUsersByDepartmentWithoutDivision() throws Exception {
        mockMvc.perform(
                        get("/api/users")
                                .param("departmentId", "1")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "andrii.employee@vyriy.com")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "petro.employee@vyriy.com")));
    }

    @Test
    void overtimeReviewSupportsDepartmentLevelView() throws Exception {
        mockMvc.perform(
                        get("/api/overtime/review")
                                .param("departmentId", "1")
                                .param("mode", "division")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "reviewOvertimeModal")));
    }

    @Test
    void adminNavigationUsesRequestedOrderAndThreeStatusLegendItems() throws Exception {
        String html =
                mockMvc.perform(
                                get("/api/overtime")
                                        .with(user("admin@vyriy.com").roles("ADMIN", "EMPLOYEE")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html.indexOf("Мої перепрацювання"))
                .isLessThan(html.indexOf("Перевірка перепрацювань"));
        org.assertj.core.api.Assertions.assertThat(html.indexOf("Перевірка перепрацювань"))
                .isLessThan(html.indexOf("Користувачі"));
        org.assertj.core.api.Assertions.assertThat(html.indexOf("Користувачі"))
                .isLessThan(html.indexOf("Керування Відділами"));
        org.assertj.core.api.Assertions.assertThat(html.indexOf("Керування Відділами"))
                .isLessThan(html.indexOf("Налаштування"));
        org.assertj.core.api.Assertions.assertThat(html.split("legend-box", -1).length - 1)
                .isEqualTo(3);
    }

    @Test
    void employeeCannotOpenBonusesButManagerCan() throws Exception {
        mockMvc.perform(get("/api/bonuses").with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/bonuses")
                                .with(user("it.manager@vyriy.com").roles("MANAGER", "EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void overtimeMatrixShowsWeekdaysAndBonusColumn() throws Exception {
        mockMvc.perform(
                        get("/api/overtime/review")
                                .param("departmentId", "1")
                                .param("divisionId", "1")
                                .param("mode", "division")
                                .param("year", "2026")
                                .param("month", "8")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("weekday-label")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Бонуси")));
    }

    @Test
    void overtimeReviewFinancialSummaryRendersDynamicCategoryColumnsAndDetails() throws Exception {
        mockMvc.perform(
                        get("/api/overtime/review")
                                .param("departmentId", "1")
                                .param("divisionId", "1")
                                .param("view", "summary")
                                .param("year", "2026")
                                .param("month", "8")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "financial-summary-table")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("categorySummary-")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("overtimeSummary-")));
    }

    @Test
    void financialSummaryIsReadOnlyForManagerAndEditableForAdmin() throws Exception {
        String managerHtml =
                mockMvc.perform(
                                get("/api/overtime/review")
                                        .param("view", "summary")
                                        .with(
                                                user("it.manager@vyriy.com")
                                                        .roles("MANAGER", "EMPLOYEE")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(managerHtml)
                .contains("financial-summary-table")
                .doesNotContain("summary-salary-form", "summary-bonus-create");

        String adminHtml =
                mockMvc.perform(
                                get("/api/overtime/review")
                                        .param("departmentId", "1")
                                        .param("divisionId", "1")
                                        .param("view", "summary")
                                        .with(user("admin@vyriy.com").roles("ADMIN", "EMPLOYEE")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(adminHtml)
                .contains("summary-salary-form", "summary-bonus-create");
    }

    @Test
    void anonymousAccessIsLimitedToAuthenticationAndAssets() throws Exception {
        mockMvc.perform(get("/api/login")).andExpect(status().isOk());
        mockMvc.perform(get("/api/register")).andExpect(status().isOk());
        mockMvc.perform(get("/api/bonuses")).andExpect(status().isForbidden());
        mockMvc.perform(get("/h2-console")).andExpect(status().isForbidden());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isForbidden());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());
    }

    @Test
    void swaggerAndOpenApiAreAvailableOnlyToAdmin() throws Exception {
        mockMvc.perform(get("/swagger-ui.html").with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v3/api-docs").with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/swagger-ui.html").with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/v3/api-docs").with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminBonusPageHasOrganizationAndStatusFilters() throws Exception {
        mockMvc.perform(
                        get("/api/bonuses")
                                .with(user("admin@vyriy.com").roles("ADMIN", "EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "name=\"departmentId\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "name=\"divisionId\"")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("name=\"status\"")));
    }

    @Test
    void managerBonusPageIsRestrictedToOwnDivision() throws Exception {
        String html =
                mockMvc.perform(
                                get("/api/bonuses")
                                        .with(
                                                user("it.manager@vyriy.com")
                                                        .roles("MANAGER", "EMPLOYEE")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(html)
                .contains("andrii.employee@vyriy.com", "maria.employee@vyriy.com");
        org.assertj.core.api.Assertions.assertThat(html).doesNotContain("petro.employee@vyriy.com");
    }

    @Test
    void resubmissionWithoutReasonFailsValidation() throws Exception {
        mockMvc.perform(
                        post("/api/overtimes/1/resubmit")
                                .with(user("employee@vyriy.com").roles("EMPLOYEE"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "workDate": "2026-08-13",
                                  "hours": 2,
                                  "description": "Опис роботи"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
