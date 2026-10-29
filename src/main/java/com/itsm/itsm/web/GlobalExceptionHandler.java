package com.itsm.itsm.web;

import com.itsm.itsm.exception.ItsmException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// If any controller throws one of our exceptions, show a friendly page
// with its message instead of a stack trace.
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ItsmException.class)
    public String handleItsmException(ItsmException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "problem";
    }
}
