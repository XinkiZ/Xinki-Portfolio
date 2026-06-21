package com.xinki.portfolio.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinki.portfolio.common.Result;
import com.xinki.portfolio.entity.KnowledgeBase;
import com.xinki.portfolio.mapper.KnowledgeBaseMapper;
import com.xinki.portfolio.service.DashScopeEmbeddingService;
import com.xinki.portfolio.service.DocumentChunkService;
import com.xinki.portfolio.service.VectorCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkService documentChunkService;
    private final VectorCacheService vectorCacheService;
    private final DashScopeEmbeddingService embeddingService;

    @PostMapping("/knowledge")
    public Result<?> createKnowledge(@RequestBody KnowledgeBase kb) {
        knowledgeBaseMapper.insert(kb);
        return Result.success(kb);
    }

    @GetMapping("/knowledge")
    public Result<?> knowledge(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(knowledgeBaseMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<KnowledgeBase>().orderByDesc(KnowledgeBase::getCreatedAt)));
    }

    @PutMapping("/knowledge/{id}")
    public Result<?> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeBase kb) {
        kb.setId(id);
        knowledgeBaseMapper.updateById(kb);
        return Result.success("已更新");
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<?> deleteKnowledge(@PathVariable Long id) {
        vectorCacheService.remove(id);
        knowledgeBaseMapper.deleteById(id);
        return Result.success("已删除");
    }

    @DeleteMapping("/knowledge/file/{sourceHash}")
    public Result<?> deleteKnowledgeFile(@PathVariable String sourceHash) {
        int deleted = knowledgeBaseMapper.delete(
                new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getSourceHash, sourceHash));
        return Result.success("已删除 " + deleted + " 条片段");
    }

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
            float[] vec = embed(chunkText);
            if (vec == null) {
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
}