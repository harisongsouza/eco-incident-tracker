package com.unip.api.action;

import com.unip.api.action.dto.MyActionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/corrective-actions")
public class MyActionsController {

    private final CorrectiveActionService service;

    public MyActionsController(CorrectiveActionService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    public List<MyActionResponse> mine() {
        return service.listMine().stream().map(MyActionResponse::from).toList();
    }
}