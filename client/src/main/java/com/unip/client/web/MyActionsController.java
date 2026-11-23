package com.unip.client.web;

import com.unip.client.api.ApiClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class MyActionsController {

    private final ApiClient api;

    public MyActionsController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/my-actions")
    public String list(@SessionAttribute(SessionUser.KEY) SessionUser user, Model model) {
        model.addAttribute("actions", api.myActions(user.getAuthorization()));
        return "my-actions";
    }
}