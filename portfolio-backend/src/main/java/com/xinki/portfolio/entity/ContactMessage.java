package com.xinki.portfolio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contact_message")
public class ContactMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String email;
    private String content;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
