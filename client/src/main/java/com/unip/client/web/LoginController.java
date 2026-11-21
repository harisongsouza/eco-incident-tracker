package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import com.unip.client.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final ApiClient api;

    public LoginController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/login")
    public String form(HttpSession session) {
        if (session.getAttribute(SessionUser.KEY) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpServletRequest request, Model model) {
        String authorization = ApiClient.basicAuth(email.trim(), password);
        try {
            UserDto user = api.me(authorization); // the API validates the credentials

            HttpSession session = request.getSession();
            request.changeSessionId(); // prevents session fixation
            session.setAttribute(SessionUser.KEY, new SessionUser(
                    user.id(), user.name(), user.email(), user.role(), authorization));
            return "redirect:/";
        } catch (ApiException e) {
            model.addAttribute("email", email);
            model.addAttribute("error",
                    e.getStatus() == 401 ? "Invalid e-mail or password" : e.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}