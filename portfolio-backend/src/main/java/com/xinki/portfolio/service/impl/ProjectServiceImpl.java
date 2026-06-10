package com.xinki.portfolio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.entity.Project;
import com.xinki.portfolio.mapper.ProjectMapper;
import com.xinki.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

    @Override
    public Page<Project> page(Integer page, Integer pageSize, String tag) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getIsPublished, 1)
                .orderByDesc(Project::getSortOrder)
                .orderByDesc(Project::getCreatedAt);
        if (tag != null && !tag.isEmpty()) {
            wrapper.like(Project::getTags, tag);
        }
        return projectMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Project getById(Long id) {
        return projectMapper.selectById(id);
    }
}