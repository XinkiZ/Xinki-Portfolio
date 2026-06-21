package com.xinki.portfolio.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.Project;
import com.xinki.portfolio.mapper.ProjectMapper;
import com.xinki.portfolio.service.ContentIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectMapper projectMapper;
    private final ContentIndexService contentIndexService;

    @GetMapping("/projects")
    public Result<?> projects(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(projectMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Project>().orderByDesc(Project::getSortOrder)));
    }

    @PostMapping("/projects")
    public Result<?> createProject(@RequestBody Project project) {
        projectMapper.insert(project);
        contentIndexService.indexProject(project);
        return Result.success(project);
    }

    @PutMapping("/projects/{id}")
    public Result<?> updateProject(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        projectMapper.updateById(project);
        Project updated = projectMapper.selectById(id);
        contentIndexService.indexProject(updated);
        return Result.success(updated);
    }

    @DeleteMapping("/projects/{id}")
    public Result<?> deleteProject(@PathVariable Long id) {
        contentIndexService.removeProjectIndex(id);
        projectMapper.deleteById(id);
        return Result.success("已删除");
    }

    @PostMapping("/projects/reindex")
    public Result<Map<String, int[]>> reindex() {
        return Result.success(contentIndexService.rebuildAll());
    }
}