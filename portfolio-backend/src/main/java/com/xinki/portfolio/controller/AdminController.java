package com.xinki.portfolio.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.dto.LoginDTO;
import com.xinki.portfolio.entity.*;
import com.xinki.portfolio.mapper.*;
import com.xinki.portfolio.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final SkillMapper skillMapper;
    private final TimelineEventMapper timelineEventMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ContactMessageMapper contactMessageMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ===== 登录 =====
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO loginDTO) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername()));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        return Result.success(data);
    }

    // ===== 个人信息 =====
    @GetMapping("/profile")
    public Result<?> profile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestHeader("Authorization") String authHeader,
                                   @RequestBody Map<String, String> body) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if (body.containsKey("username")) user.setUsername(body.get("username"));
        if (body.containsKey("avatar")) user.setAvatar(body.get("avatar"));
        if (body.containsKey("intro")) user.setIntro(body.get("intro"));
        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile/password")
    public Result<?> updatePassword(@RequestHeader("Authorization") String authHeader,
                                    @RequestBody Map<String, String> body) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.error(400, "请输入旧密码和新密码");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(400, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    // ===== 仪表盘 =====
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("projectCount", projectMapper.selectCount(null));
        data.put("skillCount", skillMapper.selectCount(null));
        data.put("messageCount", contactMessageMapper.selectCount(null));
        data.put("knowledgeCount", knowledgeBaseMapper.selectCount(null));
        data.put("unreadMessages", contactMessageMapper.selectCount(
                new LambdaQueryWrapper<ContactMessage>().eq(ContactMessage::getIsRead, 0)));
        return Result.success(data);
    }

    // ===== 作品管理 =====
    @GetMapping("/projects")
    public Result<?> projects(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Project> result = projectMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Project>().orderByDesc(Project::getCreatedAt));
        return Result.success(result);
    }

    @PostMapping("/projects")
    public Result<?> createProject(@RequestBody Project project) {
        projectMapper.insert(project);
        return Result.success("创建成功");
    }

    @PutMapping("/projects/{id}")
    public Result<?> updateProject(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        projectMapper.updateById(project);
        return Result.success("更新成功");
    }

    @DeleteMapping("/projects/{id}")
    public Result<?> deleteProject(@PathVariable Long id) {
        projectMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ===== 技能管理 =====
    @GetMapping("/skills")
    public Result<?> skills() {
        return Result.success(skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().orderByAsc(Skill::getSortOrder)));
    }

    @PostMapping("/skills")
    public Result<?> createSkill(@RequestBody Skill skill) {
        skillMapper.insert(skill);
        return Result.success("创建成功");
    }

    @PutMapping("/skills/{id}")
    public Result<?> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        skillMapper.updateById(skill);
        return Result.success("更新成功");
    }

    @DeleteMapping("/skills/{id}")
    public Result<?> deleteSkill(@PathVariable Long id) {
        skillMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ===== 时间线管理 =====
    @GetMapping("/timeline")
    public Result<?> timeline() {
        return Result.success(timelineEventMapper.selectList(
                new LambdaQueryWrapper<TimelineEvent>().orderByDesc(TimelineEvent::getStartDate)));
    }

    @PostMapping("/timeline")
    public Result<?> createTimeline(@RequestBody TimelineEvent event) {
        timelineEventMapper.insert(event);
        return Result.success("创建成功");
    }

    @PutMapping("/timeline/{id}")
    public Result<?> updateTimeline(@PathVariable Long id, @RequestBody TimelineEvent event) {
        event.setId(id);
        timelineEventMapper.updateById(event);
        return Result.success("更新成功");
    }

    @DeleteMapping("/timeline/{id}")
    public Result<?> deleteTimeline(@PathVariable Long id) {
        timelineEventMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ===== AI 知识库管理 =====
    @GetMapping("/knowledge")
    public Result<?> knowledge(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(knowledgeBaseMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<KnowledgeBase>().orderByDesc(KnowledgeBase::getCreatedAt)));
    }

    @PostMapping("/knowledge")
    public Result<?> createKnowledge(@RequestBody KnowledgeBase kb) {
        knowledgeBaseMapper.insert(kb);
        return Result.success("添加成功");
    }

    @PutMapping("/knowledge/{id}")
    public Result<?> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeBase kb) {
        kb.setId(id);
        knowledgeBaseMapper.updateById(kb);
        return Result.success("更新成功");
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<?> deleteKnowledge(@PathVariable Long id) {
        knowledgeBaseMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ===== 留言管理 =====
    @GetMapping("/messages")
    public Result<?> messages(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(contactMessageMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ContactMessage>().orderByDesc(ContactMessage::getCreatedAt)));
    }

    @PutMapping("/messages/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        ContactMessage msg = new ContactMessage();
        msg.setId(id);
        msg.setIsRead(1);
        contactMessageMapper.updateById(msg);
        return Result.success("已标记已读");
    }
}