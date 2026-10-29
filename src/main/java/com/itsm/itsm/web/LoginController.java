package com.itsm.itsm.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // Only shows the page. Spring Security handles the POST /login itself.
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
