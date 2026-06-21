package com.xinki.portfolio.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.Skill;
import com.xinki.portfolio.mapper.SkillMapper;
import com.xinki.portfolio.service.ContentIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSkillController {

    private final SkillMapper skillMapper;
    private final ContentIndexService contentIndexService;

    @GetMapping("/skills")
    public Result<?> skills() {
        return Result.success(skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().orderByAsc(Skill::getSortOrder)));
    }

    @PostMapping("/skills")
    public Result<?> createSkill(@RequestBody Skill skill) {
        skillMapper.insert(skill);
        contentIndexService.indexSkill(skill);
        return Result.success(skill);
    }

    @PutMapping("/skills/{id}")
    public Result<?> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        skillMapper.updateById(skill);
        Skill updated = skillMapper.selectById(id);
        contentIndexService.indexSkill(updated);
        return Result.success(updated);
    }

    @DeleteMapping("/skills/{id}")
    public Result<?> deleteSkill(@PathVariable Long id) {
        contentIndexService.removeSkillIndex(id);
        skillMapper.deleteById(id);
        return Result.success("已删除");
    }
}