package com.xinki.portfolio.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.TimelineEvent;
import com.xinki.portfolio.mapper.TimelineEventMapper;
import com.xinki.portfolio.service.ContentIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTimelineController {

    private final TimelineEventMapper timelineEventMapper;
    private final ContentIndexService contentIndexService;

    @GetMapping("/timeline")
    public Result<?> timeline() {
        return Result.success(timelineEventMapper.selectList(
                new LambdaQueryWrapper<TimelineEvent>().orderByDesc(TimelineEvent::getStartDate)));
    }

    @PostMapping("/timeline")
    public Result<?> createTimeline(@RequestBody TimelineEvent event) {
        timelineEventMapper.insert(event);
        contentIndexService.indexTimeline(event);
        return Result.success(event);
    }

    @PutMapping("/timeline/{id}")
    public Result<?> updateTimeline(@PathVariable Long id, @RequestBody TimelineEvent event) {
        event.setId(id);
        timelineEventMapper.updateById(event);
        TimelineEvent updated = timelineEventMapper.selectById(id);
        contentIndexService.indexTimeline(updated);
        return Result.success(updated);
    }

    @DeleteMapping("/timeline/{id}")
    public Result<?> deleteTimeline(@PathVariable Long id) {
        contentIndexService.removeTimelineIndex(id);
        timelineEventMapper.deleteById(id);
        return Result.success("已删除");
    }
}