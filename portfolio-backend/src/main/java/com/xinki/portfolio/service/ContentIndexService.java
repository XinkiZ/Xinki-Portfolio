package com.xinki.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinki.portfolio.entity.*;
import com.xinki.portfolio.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xinki.portfolio.service.DashScopeEmbeddingService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Auto-index projects, skills, and timeline events into knowledge_base for RAG.
 * source_hash prefixes: "project:", "skill:", "timeline:"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentIndexService {

    private final ProjectMapper projectMapper;
    private final SkillMapper skillMapper;
    private final TimelineEventMapper timelineEventMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DashScopeEmbeddingService embeddingService;
    private final VectorCacheService vectorCacheService;

    // ==================== Project ====================

    public void indexProject(Project p) {
        if (p.getIsPublished() != null && p.getIsPublished() != 1) {
            removeByHash("project:" + p.getId());
            return;
        }
        indexContent("project:" + p.getId(), buildProjectContent(p), "作品:" + p.getTitle(), "project");
    }

    public void removeProjectIndex(Long id) { removeByHash("project:" + id); }

    // ==================== Skill ====================

    public void indexSkill(Skill s) {
        indexContent("skill:" + s.getId(), buildSkillContent(s), "技能:" + s.getName(), "skill");
    }

    public void removeSkillIndex(Long id) { removeByHash("skill:" + id); }

    // ==================== Timeline ====================

    public void indexTimeline(TimelineEvent e) {
        indexContent("timeline:" + e.getId(), buildTimelineContent(e), "经历:" + e.getTitle(), "timeline");
    }

    public void removeTimelineIndex(Long id) { removeByHash("timeline:" + id); }

    // ==================== Batch ====================

    /** Rebuild all indexes. Returns {project:[total,ok,fail], skill:[...], timeline:[...]} */
    public Map<String, int[]> rebuildAll() {
        Map<String, int[]> result = new LinkedHashMap<>();
        result.put("project", rebuildProjects());
        result.put("skill", rebuildSkills());
        result.put("timeline", rebuildTimeline());
        return result;
    }

    // ==================== Internal ====================


    private float[] embed(String text) {
        return embeddingService.embedDocument(text);
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
    private void indexContent(String hash, String content, String sourceFile, String category) {
        removeByHash(hash);
        float[] vec = embed(content);
        if (vec == null) {
            log.warn("Embedding failed for {}", hash);
            return;
        }
        KnowledgeBase kb = new KnowledgeBase();
        kb.setContent(content);
        kb.setEmbedding(serializeVec(vec));
        kb.setSourceFile(sourceFile);
        kb.setSourceHash(hash);
        kb.setChunkIndex(0);
        kb.setCategory(category);
        knowledgeBaseMapper.insert(kb);
        vectorCacheService.put(kb.getId(), vec);
        vectorCacheService.putHashMapping(hash, List.of(kb.getId()));
        log.debug("Indexed: {}", hash);
    }

    private void removeByHash(String hash) {
        List<KnowledgeBase> existing = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getSourceHash, hash));
        for (KnowledgeBase kb : existing) {
            vectorCacheService.remove(kb.getId());
            knowledgeBaseMapper.deleteById(kb.getId());
        }
        if (!existing.isEmpty()) vectorCacheService.removeHashMapping(hash);
    }

    private int[] rebuildProjects() {
        List<Project> list = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getIsPublished, 1));
        int ok = 0, fail = 0;
        for (Project p : list) { try { removeByHash("project:" + p.getId()); indexProject(p); ok++; } catch (Exception ex) { fail++; log.error("Index project {} failed", p.getId(), ex); } }
        return new int[]{list.size(), ok, fail};
    }

    private int[] rebuildSkills() {
        List<Skill> list = skillMapper.selectList(null);
        int ok = 0, fail = 0;
        for (Skill s : list) { try { removeByHash("skill:" + s.getId()); indexSkill(s); ok++; } catch (Exception ex) { fail++; log.error("Index skill {} failed", s.getId(), ex); } }
        return new int[]{list.size(), ok, fail};
    }

    private int[] rebuildTimeline() {
        List<TimelineEvent> list = timelineEventMapper.selectList(null);
        int ok = 0, fail = 0;
        for (TimelineEvent e : list) { try { removeByHash("timeline:" + e.getId()); indexTimeline(e); ok++; } catch (Exception ex) { fail++; log.error("Index timeline {} failed", e.getId(), ex); } }
        return new int[]{list.size(), ok, fail};
    }

    // ==================== Content builders ====================

    private String buildProjectContent(Project p) {
        StringBuilder sb = new StringBuilder("【作品】" + p.getTitle());
        if (p.getSummary() != null && !p.getSummary().isEmpty()) sb.append("\n简介：").append(p.getSummary());
        if (p.getDescription() != null && !p.getDescription().isEmpty()) sb.append("\n详情：").append(p.getDescription());
        if (p.getTags() != null && !p.getTags().isEmpty()) sb.append("\n标签：").append(p.getTags());
        return sb.toString();
    }

    private String buildSkillContent(Skill s) {
        StringBuilder sb = new StringBuilder("【技能】" + s.getName());
        if (s.getCategory() != null && !s.getCategory().isEmpty()) sb.append("（").append(s.getCategory()).append("）");
        if (s.getLevel() != null) sb.append("\n熟练度：").append(s.getLevel()).append("%");
        return sb.toString();
    }

    private String buildTimelineContent(TimelineEvent e) {
        StringBuilder sb = new StringBuilder("【经历】" + e.getTitle());
        String typeLabel = switch (e.getType()) {
            case "education" -> "教育";
            case "work" -> "工作";
            case "project" -> "项目";
            default -> e.getType();
        };
        sb.append("\n类型：").append(typeLabel);
        if (e.getStartDate() != null) {
            sb.append("\n时间：").append(e.getStartDate());
            if (e.getEndDate() != null) sb.append(" 至 ").append(e.getEndDate());
        }
        if (e.getDescription() != null && !e.getDescription().isEmpty()) sb.append("\n详情：").append(e.getDescription());
        return sb.toString();
    }
}