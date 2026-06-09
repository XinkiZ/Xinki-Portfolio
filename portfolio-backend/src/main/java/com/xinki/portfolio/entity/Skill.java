package com.xinki.portfolio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("skill")
public class Skill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private Integer level;
    private String icon;
    private Integer sortOrder;
}
