package com.xinki.portfolio.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.Skill;
import com.xinki.portfolio.entity.TimelineEvent;
import com.xinki.portfolio.entity.User;
import com.xinki.portfolio.mapper.SkillMapper;
import com.xinki.portfolio.mapper.TimelineEventMapper;
import com.xinki.portfolio.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/about")
@RequiredArgsConstructor
public class AboutController {

    private final UserMapper userMapper;
    private final SkillMapper skillMapper;
    private final TimelineEventMapper timelineEventMapper;

    @GetMapping
    public Result<Map<String, Object>> about() {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>());
        if (user != null) user.setPassword(null);
        result.put("user", user);

        List<Skill> skills = skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().orderByAsc(Skill::getSortOrder));
        result.put("skills", skills);

        List<TimelineEvent> timeline = timelineEventMapper.selectList(
                new LambdaQueryWrapper<TimelineEvent>().orderByDesc(TimelineEvent::getStartDate));
        result.put("timeline", timeline);

        return Result.success(result);
    }
}
