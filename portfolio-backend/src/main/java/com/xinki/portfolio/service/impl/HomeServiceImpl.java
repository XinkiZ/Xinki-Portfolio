package com.xinki.portfolio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.entity.Project;
import com.xinki.portfolio.entity.Skill;
import com.xinki.portfolio.entity.TimelineEvent;
import com.xinki.portfolio.entity.User;
import com.xinki.portfolio.mapper.ProjectMapper;
import com.xinki.portfolio.mapper.SkillMapper;
import com.xinki.portfolio.mapper.TimelineEventMapper;
import com.xinki.portfolio.mapper.UserMapper;
import com.xinki.portfolio.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final SkillMapper skillMapper;
    private final TimelineEventMapper timelineEventMapper;

    @Override
    public Map<String, Object> getHomeInfo() {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().last("LIMIT 1"));
        if (user != null) {
            user.setPassword(null);
        }
        result.put("user", user);

        Page<Project> page = projectMapper.selectPage(
                new Page<>(1, 6),
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getIsPublished, 1)
                        .orderByDesc(Project::getCreatedAt));
        result.put("recentProjects", page.getRecords());

        List<Skill> skills = skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().orderByAsc(Skill::getSortOrder));
        result.put("skills", skills);

        List<TimelineEvent> timeline = timelineEventMapper.selectList(
                new LambdaQueryWrapper<TimelineEvent>().orderByDesc(TimelineEvent::getStartDate));
        result.put("timeline", timeline);

        return result;
    }
}