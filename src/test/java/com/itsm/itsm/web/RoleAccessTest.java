package com.itsm.itsm.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                "/admin/categories", "/admin/sla", "/admin/audit", "/admin/users/technicians/new"}) {
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
}
