package com.xinki.portfolio.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.entity.Project;

public interface ProjectService {
    Page<Project> page(Integer pageNum, Integer pageSize, String tag);
    Project getById(Long id);
}
