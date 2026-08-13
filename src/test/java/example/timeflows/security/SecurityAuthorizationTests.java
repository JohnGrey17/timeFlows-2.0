package example.timeflows.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageContainsCsrfToken() throws Exception {
        mockMvc.perform(get("/api/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void stateChangingRequestWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .with(user("admin@vyriy.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Без CSRF\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void spaRequestAcceptsRawCookieTokenInHeader() throws Exception {
        MvcResult page = mockMvc.perform(get("/api/login"))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie csrfCookie = page.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/departments")
                        .with(user("admin@vyriy.com").roles("ADMIN"))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void employeeCannotCreateDepartmentEvenWithCsrf() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .with(user("employee@vyriy.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Заборонено\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPassesCsrfAndRoleChecksForDepartmentMutation() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .with(user("admin@vyriy.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerCanReadDepartments() throws Exception {
        mockMvc.perform(get("/api/departments")
                        .with(user("manager@vyriy.com").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotReadUsersRestApi() throws Exception {
        mockMvc.perform(get("/api/users/rest")
                        .with(user("employee@vyriy.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void resubmissionWithoutReasonFailsValidation() throws Exception {
        mockMvc.perform(post("/api/overtimes/1/resubmit")
                        .with(user("employee@vyriy.com").roles("EMPLOYEE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workDate": "2026-08-13",
                                  "hours": 2,
                                  "description": "Опис роботи"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
