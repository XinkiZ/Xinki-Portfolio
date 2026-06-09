package com.xinki.portfolio.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("timeline_event")
public class TimelineEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer sortOrder;
}
