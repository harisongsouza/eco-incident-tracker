package com.unip.client.web;

import com.unip.client.api.ApiException;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    /** Makes the logged user available to every template as ${currentUser}. */
    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpSession session) {
        return (SessionUser) session.getAttribute(SessionUser.KEY);
    }

    @ExceptionHandler(ApiException.class)
    public String handleApiException(ApiException ex, HttpSession session, Model model) {
        if (ex.getStatus() == 401) { // credentials no longer valid
            session.invalidate();
            return "redirect:/login";
        }
        model.addAttribute("currentUser", session.getAttribute(SessionUser.KEY));
        model.addAttribute("status", ex.getStatus());
        model.addAttribute("message", ex.getMessage());
        return "api-error.html";
    }
}