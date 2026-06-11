package com.xinki.portfolio.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.dto.LoginDTO;
import com.xinki.portfolio.entity.*;
import com.xinki.portfolio.mapper.*;
import com.xinki.portfolio.service.DocumentChunkService;
import com.xinki.portfolio.service.ContentIndexService;
import com.xinki.portfolio.service.VectorCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final DocumentChunkService documentChunkService;
    private final ContentIndexService contentIndexService;
    private final VectorCacheService vectorCacheService;
    private final StringRedisTemplate redisTemplate;
    private final com.xinki.portfolio.util.JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";

    // ===== 登录（含暴力破解防护） =====
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String failKey = LOGIN_FAIL_PREFIX + clientIp;

        // 检查是否已被锁定
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;
        if (failCount >= LOGIN_MAX_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(failKey);
            long minutes = (ttl != null && ttl > 0) ? ttl / 60 : 15;
            return Result.error(429, "登录尝试次数过多，请 " + minutes + " 分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername()));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            // 记录失败次数
            failCount++;
            redisTemplate.opsForValue().set(failKey, String.valueOf(failCount), LOGIN_LOCK_DURATION);
            int remaining = LOGIN_MAX_ATTEMPTS - failCount;
            return Result.error(401, "用户名或密码错误" + (remaining > 0 ? "，还剩 " + remaining + " 次尝试" : "，账号已锁定"));
        }

        // 登录成功，清除失败记录
        redisTemplate.delete(failKey);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        return Result.success(data);
    }

    // ===== 个人信息（认证由 JwtInterceptor 处理） =====
    @GetMapping("/profile")
    public Result<?> profile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request,
                                   @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
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
    public Result<?> updatePassword(HttpServletRequest request,
                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
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
        return Result.success(data);
    }

    // ===== 作品管理 CRUD（ContentIndexService 联动已发布→索引，未发布→删除索引） =====
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
        if (project.getIsPublished() != null && project.getIsPublished() == 1) {
            contentIndexService.indexProject(project);
        }
        return Result.success("添加成功");
    }

    @PutMapping("/projects/{id}")
    public Result<?> updateProject(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        projectMapper.updateById(project);
        if (project.getIsPublished() != null && project.getIsPublished() == 1) {
            contentIndexService.indexProject(project);
        } else {
            contentIndexService.removeProjectIndex(id);
        }
        return Result.success("更新成功");
    }

    @DeleteMapping("/projects/{id}")
    public Result<?> deleteProject(@PathVariable Long id) {
        projectMapper.deleteById(id);
        contentIndexService.removeProjectIndex(id);
        return Result.success("删除成功");
    }

    @PostMapping("/projects/reindex")
    public Result<?> reindexProjects() {
        contentIndexService.rebuildAll();
        return Result.success("全量重建索引已完成");
    }

    // ===== 技能管理 CRUD =====
    @GetMapping("/skills")
    public Result<?> skills(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(skillMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Skill>().orderByAsc(Skill::getSortOrder)));
    }

    @PostMapping("/skills")
    public Result<?> createSkill(@RequestBody Skill skill) {
        skillMapper.insert(skill);
        contentIndexService.indexSkill(skill);
        return Result.success("添加成功");
    }

    @PutMapping("/skills/{id}")
    public Result<?> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        skillMapper.updateById(skill);
        contentIndexService.indexSkill(skill);
        return Result.success("更新成功");
    }

    @DeleteMapping("/skills/{id}")
    public Result<?> deleteSkill(@PathVariable Long id) {
        skillMapper.deleteById(id);
        contentIndexService.removeSkillIndex(id);
        return Result.success("删除成功");
    }

    // ===== 时间线管理 CRUD =====
    @GetMapping("/timeline")
    public Result<?> timeline(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(timelineEventMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<TimelineEvent>().orderByDesc(TimelineEvent::getSortOrder)));
    }

    @PostMapping("/timeline")
    public Result<?> createTimeline(@RequestBody TimelineEvent event) {
        timelineEventMapper.insert(event);
        contentIndexService.indexTimeline(event);
        return Result.success("添加成功");
    }

    @PutMapping("/timeline/{id}")
    public Result<?> updateTimeline(@PathVariable Long id, @RequestBody TimelineEvent event) {
        event.setId(id);
        timelineEventMapper.updateById(event);
        contentIndexService.indexTimeline(event);
        return Result.success("更新成功");
    }

    @DeleteMapping("/timeline/{id}")
    public Result<?> deleteTimeline(@PathVariable Long id) {
        timelineEventMapper.deleteById(id);
        contentIndexService.removeTimelineIndex(id);
        return Result.success("删除成功");
    }

    // ===== 知识库管理 =====
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

    /** Delete all chunks belonging to a source file (by hash). */
    @DeleteMapping("/knowledge/file/{sourceHash}")
    public Result<?> deleteKnowledgeFile(@PathVariable String sourceHash) {
        int deleted = knowledgeBaseMapper.delete(
                new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getSourceHash, sourceHash));
        return Result.success("已删除 " + deleted + " 条片段");
    }


    private String serializeVec(float[] vec) {
        if (vec == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] deserializeVec(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            com.fasterxml.jackson.databind.JsonNode arr = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            float[] result = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                result[i] = (float) arr.get(i).asDouble();
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
    /** Import document: parse → chunk → embed → store. */
    @PostMapping("/knowledge/import")
    public Result<?> importKnowledge(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择文件");
        }
        String filename = file.getOriginalFilename();

        String sha256 = documentChunkService.computeHash(file);
        List<KnowledgeBase> existingDB = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getSourceHash, sha256));
        if (!existingDB.isEmpty()) {
            for (KnowledgeBase kb : existingDB) {
                vectorCacheService.remove(kb.getId());
            }
            knowledgeBaseMapper.delete(
                    new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getSourceHash, sha256));
            vectorCacheService.removeHashMapping(sha256);
        }

        List<String> chunks = documentChunkService.chunkFile(file);
        if (chunks.isEmpty()) {
            return Result.error(400, "未能从文件中提取到有效文本（可能为空、扫描版 PDF 或不支持的格式）");
        }

        int successCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        List<Long> chunkIds = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            float[] vec = if (vec == null) {
                errors.add(Map.of("chunkIndex", i, "reason", "Embedding 生成失败，已跳过"));
                continue;
            }

            KnowledgeBase kb = new KnowledgeBase();
            kb.setContent(chunkText);
            kb.setEmbedding(serializeVec(vec));
            kb.setSourceFile(filename);
            kb.setSourceHash(sha256);
            kb.setChunkIndex(i);
            knowledgeBaseMapper.insert(kb);

            vectorCacheService.put(kb.getId(), vec);
            chunkIds.add(kb.getId());
            successCount++;
        }

        if (!chunkIds.isEmpty()) {
            vectorCacheService.putHashMapping(sha256, chunkIds);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fileName", filename);
        result.put("totalChunks", chunks.size());
        result.put("successCount", successCount);
        result.put("failedCount", errors.size());
        result.put("errors", errors);
        return Result.success(result);
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

    // ===== 工具方法 =====
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}