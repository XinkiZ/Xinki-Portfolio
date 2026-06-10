package com.xinki.portfolio.controller;

import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.ContactMessage;
import com.xinki.portfolio.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public Result<?> submit(@RequestBody ContactMessage message) {
        contactService.submit(message);
        return Result.success("留言已发送");
    }
}
