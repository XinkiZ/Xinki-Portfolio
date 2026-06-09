package com.xinki.portfolio.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.Project;
import com.xinki.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public Result<Page<Project>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "9") Integer pageSize,
            @RequestParam(required = false) String tag) {
        return Result.success(projectService.page(page, pageSize, tag));
    }

    @GetMapping("/{id}")
    public Result<Project> detail(@PathVariable Long id) {
        Project project = projectService.getById(id);
        if (project == null) {
            return Result.error(404, "项目不存在");
        }
        return Result.success(project);
    }
}
