package example.timeflows.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
            "timeflows.demo-data.enabled=false",
            "timeflows.mfa.enabled=true"
        })
@AutoConfigureMockMvc
@Import(SecurityTestData.class)
class SecurityAuthorizationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private example.timeflows.service.UserService userService;
    @Autowired private example.timeflows.repository.BonusRepository bonusRepository;

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
    void registrationMfaUsesPendingUserInsteadOfExistingAuthenticatedUser() throws Exception {
        MvcResult registration =
                mockMvc.perform(
                                post("/api/register")
                                        .with(user("admin@vyriy.com").roles("ADMIN"))
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("firstName", "Pending")
                                        .param("lastName", "Employee")
                                        .param("email", "pending.employee@vyriy.com")
                                        .param("password", "test-password")
                                        .param("divisionId", "1"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();

        jakarta.servlet.http.Cookie pendingCookie =
                registration.getResponse().getCookie("TIMEFLOWS_MFA_PENDING");

        mockMvc.perform(
                        get("/api/mfa/setup")
                                .with(user("admin@vyriy.com").roles("ADMIN"))
                                .cookie(pendingCookie))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                                .attribute("email", "pending.employee@vyriy.com"));
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
    void employeeCannotCreateOvertimeForAnotherDivisionUser() throws Exception {
        mockMvc.perform(
                        post("/api/overtimes/users/1")
                                .with(user("employee@vyriy.com").roles("EMPLOYEE"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"workDate\":\"2026-08-15\",\"hours\":2,\"description\":\"Робота\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUserManagementShowsDivisionOvertimeTagHelp() throws Exception {
        mockMvc.perform(get("/api/users").with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("DIVISION OVERTIME")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Тег працює в зв'язці з ролю менеджер")));
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
    void adminCanSaveAndReuseOwnOvertimeFilterInBothViews() throws Exception {
        String filterName = "IT DEV за серпень";

        mockMvc.perform(
                        post("/api/overtime/review/filters")
                                .with(user("admin@vyriy.com").roles("ADMIN"))
                                .with(csrf())
                                .param("name", filterName)
                                .param("departmentId", "1")
                                .param("divisionId", "1")
                                .param("status", "APPROVED_MANAGER")
                                .param("month", "8")
                                .param("year", "2026")
                                .param("view", "matrix"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        header().string(
                                        "Location",
                                        org.hamcrest.Matchers.containsString("divisionId=1")))
                .andExpect(
                        header().string(
                                        "Location",
                                        org.hamcrest.Matchers.containsString("view=matrix")));

        mockMvc.perform(
                        get("/api/overtime/review")
                                .param("view", "matrix")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(filterName)))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("Зберегти фільтр")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "У вас є змога зберігти вже налаштований фільтр")));

        mockMvc.perform(
                        get("/api/overtime/review")
                                .param("view", "summary")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(filterName)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("view=summary")));
    }

    @Test
    void managerCannotSaveAdminOvertimeFilter() throws Exception {
        mockMvc.perform(
                        post("/api/overtime/review/filters")
                                .with(user("it.manager@vyriy.com").roles("MANAGER"))
                                .with(csrf())
                                .param("name", "Недоступний фільтр")
                                .param("departmentId", "1")
                                .param("month", "8")
                                .param("year", "2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationShowsDivisionManagerAndAdminReviewLink() throws Exception {
        mockMvc.perform(get("/api/organization").with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Керівник:")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "it.manager@vyriy.com")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "/api/overtime/review?mode=division")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("divisionId=")));
    }

    @Test
    void sysAdminDoesNotReceiveDivisionReviewLink() throws Exception {
        mockMvc.perform(get("/api/organization").with(user("admin@vyriy.com").roles("SYS_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "/api/overtime/review?mode=division"))));
    }

    @Test
    void employeeCannotOpenOrganizationManagement() throws Exception {
        mockMvc.perform(get("/api/organization").with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
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
    void adminNavigationUsesRequestedOrderAndFourStatusLegendItems() throws Exception {
        String html =
                mockMvc.perform(
                                get("/api/overtime")
                                        .with(user("admin@vyriy.com").roles("ADMIN", "EMPLOYEE")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(html.indexOf("/api/overtime\""))
                .isLessThan(html.indexOf("/api/overtime/review"));
        org.assertj.core.api.Assertions.assertThat(html.indexOf("/api/overtime/review"))
                .isLessThan(html.indexOf("/api/users"));
        org.assertj.core.api.Assertions.assertThat(html.indexOf("/api/users"))
                .isLessThan(html.indexOf("/api/organization"));
        org.assertj.core.api.Assertions.assertThat(html.indexOf("/api/organization"))
                .isLessThan(html.indexOf("/api/settings"));
        org.assertj.core.api.Assertions.assertThat(html.split("legend-box", -1).length - 1)
                .isEqualTo(4);
    }

    @Test
    void onlyAdminCanOpenBonusModule() throws Exception {
        mockMvc.perform(
                        get("/api/bonuses")
                                .with(user("andrii.employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/bonuses")
                                .with(user("it.manager@vyriy.com").roles("MANAGER", "EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/bonuses")
                                .with(user("project.lead@vyriy.com").roles("MANAGER", "EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/bonuses")
                                .with(user("admin@vyriy.com").roles("ADMIN", "EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void adminModulesUseCurrentDatabaseRolesWhenAuthorityIsStale() throws Exception {
        mockMvc.perform(get("/api/bonuses").with(user("admin@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users").with(user("admin@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void managerCannotDeactivateOwnAccount() throws Exception {
        Long managerId = userService.findByEmail("it.manager@vyriy.com").getId();
        mockMvc.perform(
                        post("/api/users/{id}/deactivate", managerId)
                                .with(csrf())
                                .with(user("it.manager@vyriy.com").roles("MANAGER", "EMPLOYEE"))
                                .param("reason", "Помилкова самодеактивація"))
                .andExpect(status().isBadRequest());
        org.assertj.core.api.Assertions.assertThat(
                        userService.findByEmail("it.manager@vyriy.com").isActive())
                .isTrue();
    }

    @Test
    void managerCanOpenUsersForOwnDivision() throws Exception {
        mockMvc.perform(
                        get("/api/users")
                                .with(user("it.manager@vyriy.com").roles("MANAGER", "EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "andrii.employee@vyriy.com")));
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
    void adminExportsCurrentOvertimeSummarySelectionToExcel() throws Exception {
        mockMvc.perform(
                        get("/api/overtime/review/export")
                                .param("departmentId", "1")
                                .param("divisionId", "1")
                                .param("year", "2026")
                                .param("month", "8")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Content-Type",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(
                        result ->
                                org.assertj.core.api.Assertions.assertThat(
                                                result.getResponse().getContentAsByteArray())
                                        .isNotEmpty());
    }

    @Test
    void overtimeReviewShowsFinancialSummaryWithHoursButWithoutSalaryFormula() throws Exception {
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
                                                "Години перепрацювань")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "financial-summary-table")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("Співробітники = ")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("row-number-column")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Базова ставка"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Сума до сплати"))));
    }

    @Test
    void overtimeCalendarShowsFilteredEmployeeCountAndRowNumbers() throws Exception {
        mockMvc.perform(
                        get("/api/overtime/review")
                                .param("departmentId", "1")
                                .param("divisionId", "1")
                                .param("view", "matrix")
                                .param("year", "2026")
                                .param("month", "8")
                                .with(user("admin@vyriy.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "overtime-selection-count")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("Співробітники = ")))
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("row-number-column")));
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
                .contains("financial-summary-table", "summary-bonus-create")
                .doesNotContain("summary-salary-form");
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
    void managerCannotOpenBonusPageEvenWithOrganizationFilter() throws Exception {
        mockMvc.perform(
                        get("/api/bonuses")
                                .param("divisionId", "1")
                                .with(user("it.manager@vyriy.com").roles("MANAGER", "EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanApproveAndRejectPendingBonusFromBonusModule() throws Exception {
        example.timeflows.model.Bonus bonus =
                bonusRepository.findAll().stream()
                        .filter(b -> b.getStatus() == example.timeflows.model.BonusStatus.PENDING)
                        .findFirst()
                        .orElseThrow();
        try {
            mockMvc.perform(
                            post("/api/bonuses/{id}/approve", bonus.getId())
                                    .with(csrf())
                                    .with(user("admin@vyriy.com").roles("ADMIN", "EMPLOYEE")))
                    .andExpect(status().is3xxRedirection());

            bonus.setStatus(example.timeflows.model.BonusStatus.PENDING);
            bonusRepository.save(bonus);

            mockMvc.perform(
                            post("/api/bonuses/{id}/reject", bonus.getId())
                                    .with(csrf())
                                    .with(user("admin@vyriy.com").roles("ADMIN", "EMPLOYEE"))
                                    .param("comment", "Відхилено адміністратором"))
                    .andExpect(status().is3xxRedirection());
        } finally {
            bonus.setStatus(example.timeflows.model.BonusStatus.PENDING);
            bonus.setAdminComment(null);
            bonusRepository.save(bonus);
        }
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
