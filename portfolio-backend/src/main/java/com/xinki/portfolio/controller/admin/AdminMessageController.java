package com.xinki.portfolio.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.ContactMessage;
import com.xinki.portfolio.mapper.ContactMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMessageController {

    private final ContactMessageMapper contactMessageMapper;

    @GetMapping("/messages")
    public Result<?> messages(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(contactMessageMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ContactMessage>().orderByDesc(ContactMessage::getCreatedAt)));
    }

    @PutMapping("/messages/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        ContactMessage msg = new ContactMessage();
        msg.setId(id);
        msg.setIsRead(1);
        contactMessageMapper.updateById(msg);
        return Result.success("已标记已读");
    }
}