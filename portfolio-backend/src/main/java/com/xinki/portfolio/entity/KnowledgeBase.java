package com.xinki.portfolio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private String embedding;
    private String sourceFile;
    private String sourceHash;
    private Integer chunkIndex;
    private String category;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}