package com.xinki.portfolio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private String demoUrl;
    private String githubUrl;
    private String tags;
    private Integer sortOrder;
    private Integer isPublished;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
