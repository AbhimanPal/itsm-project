package com.itsm.itsm.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// Authorization tests: MockMvc sends fake browser requests, and @WithMockUser
// pretends someone with that role is logged in.
@SpringBootTest
@AutoConfigureMockMvc
class RoleAccessTest {

    @Autowired MockMvc mvc;

    @Test
    void notLoggedInIsSentToLoginPage() throws Exception {
        mvc.perform(get("/admin/incidents")).andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "asha@itsm.local", roles = "EMPLOYEE")
    void employeeCannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ravi@itsm.local", roles = "TECHNICIAN")
    void technicianCannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin/sla")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@itsm.local", roles = "ADMIN")
    void adminCanOpenEveryAdminPage() throws Exception {
        for (String page : new String[]{"/admin", "/admin/incidents", "/admin/users", "/admin/services",
                "/admin/categories", "/admin/sla", "/admin/audit", "/admin/users/technicians/new", "/admin/live"}) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(username = "admin@itsm.local", roles = "ADMIN")
    void filtersInTheUrlAreAccepted() throws Exception {
        mvc.perform(get("/admin/incidents").param("status", "NEW").param("priority", "P1")
                        .param("from", "2026-11-01").param("technicianId", "0").param("slaStatus", ""))
                .andExpect(status().isOk());
    }

    // Live charts: the JSON has one list of labels and one list of numbers per chart
    @Test
    @WithMockUser(username = "admin@itsm.local", roles = "ADMIN")
    void adminGetsChartDataAsJson() throws Exception {
        mvc.perform(get("/admin/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.byStatus.labels.length()").value(9))    // one bar per status
                .andExpect(jsonPath("$.byPriority.labels[0]").value("P1 Critical"))
                .andExpect(jsonPath("$.technicianWorkload.labels").isArray());
    }

    @Test
    @WithMockUser(username = "asha@itsm.local", roles = "EMPLOYEE")
    void employeeCannotReadChartData() throws Exception {
        mvc.perform(get("/admin/api/stats")).andExpect(status().isForbidden());
    }

    // Logged in as someone who isn't in the database (e.g. after it was reset): InvalidLoginException page
    @Test
    @WithMockUser(username = "ghost@itsm.local", roles = "EMPLOYEE")
    void unknownLoggedInUserGetsFriendlyErrorPage() throws Exception {
        mvc.perform(get("/employee/incidents"))
                .andExpect(status().isOk())
                .andExpect(view().name("problem"));
    }

    @Test
    @WithMockUser(username = "asha@itsm.local", roles = "EMPLOYEE")
    void employeeCannotOpenDatabaseConsole() throws Exception {
        mvc.perform(get("/h2-console")).andExpect(status().isForbidden());
    }
}
