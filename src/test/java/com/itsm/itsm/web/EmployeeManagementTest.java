package com.itsm.itsm.web;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.Employee;
import com.itsm.itsm.repository.EmployeeRepository;
import com.itsm.itsm.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The admin manages employees: edit details, deactivate, reactivate.
// formLogin() sends a real login form, so these tests go through Spring Security's password check.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeManagementTest {

    @Autowired MockMvc mvc;
    @Autowired AdminUserService userService;
    @Autowired EmployeeRepository employeeRepository;

    private Employee asha() {
        return employeeRepository.findByEmail("asha@itsm.local").orElseThrow();
    }

    private void logInAsAsha(String expectedUrl) throws Exception {
        mvc.perform(formLogin().userParameter("email").user("asha@itsm.local").password("demo1234"))
                .andExpect(redirectedUrl(expectedUrl));
    }

    @Test
    void deactivatedEmployeeCannotLogInUntilReactivated() throws Exception {
        logInAsAsha("/");                                     // works normally

        userService.setEmployeeActive(asha().getId(), false);
        logInAsAsha("/login?deactivated");                    // refused, with its own message

        userService.setEmployeeActive(asha().getId(), true);
        logInAsAsha("/");                                     // works again
    }

    @Test
    void adminCanCorrectNameAndDepartment() {
        userService.updateEmployee(asha().getId(), "  Asha R. Rao ", "Accounts");
        assertEquals("Asha R. Rao", asha().getName());
        assertEquals("Accounts", asha().getDepartment());
    }

    @Test
    void emptyDepartmentIsRejected() {
        Long id = asha().getId();
        assertThrows(InvalidInputException.class, () -> userService.updateEmployee(id, "Asha", " "));
    }

    @Test
    @WithMockUser(username = "admin@itsm.local", roles = "ADMIN")
    void adminCanOpenTheEditEmployeePage() throws Exception {
        mvc.perform(get("/admin/users/employees/" + asha().getId())).andExpect(status().isOk());
    }
}
