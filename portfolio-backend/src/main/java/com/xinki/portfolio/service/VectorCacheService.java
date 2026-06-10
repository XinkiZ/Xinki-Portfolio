package com.xinki.portfolio.service;

import com.xinki.portfolio.entity.KnowledgeBase;
import com.xinki.portfolio.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed embedding cache with graceful fallback to in-memory.
 *
 * Redis key:   knowledge:emb:{id}  → JSON float array
 * Redis key:   knowledge:hash:{sha256} → comma-separated chunk IDs (dedup)
 * TTL: 7 days (refreshed on access)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorCacheService {

    private final StringRedisTemplate redisTemplate;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final EmbeddingService embeddingService;

    private static final String EMB_PREFIX = "knowledge:emb:";
    private static final String HASH_PREFIX = "knowledge:hash:";
    private static final Duration TTL = Duration.ofDays(7);

    /** In-memory fallback when Redis is down. */
    private final Map<Long, float[]> localCache = new ConcurrentHashMap<>();

    /** Whether Redis is available. */
    private volatile boolean redisAvailable = true;

    @PostConstruct
    public void warmUp() {
        try {
            redisTemplate.opsForValue().get("health:check");
            redisAvailable = true;
            log.info("Redis connected, vector cache ready");
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis unavailable, using in-memory fallback for vector cache");
            loadAllToLocal();
        }
    }

    /** Store embedding for a knowledge entry. */
    public void put(Long id, float[] vec) {
        String json = embeddingService.serialize(vec);
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(EMB_PREFIX + id, json, TTL);
                return;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis write failed, switching to local cache");
            }
        }
        localCache.put(id, vec);
    }

    /** Get embedding for a knowledge entry by id. */
    public float[] get(Long id) {
        if (redisAvailable) {
            try {
                String json = redisTemplate.opsForValue().get(EMB_PREFIX + id);
                if (json != null) return embeddingService.deserialize(json);
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis read failed, switching to local cache");
                loadAllToLocal();
            }
        }
        float[] vec = localCache.get(id);
        if (vec != null) return vec;
        // Lazy load from DB
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb != null && kb.getEmbedding() != null) {
            vec = embeddingService.deserialize(kb.getEmbedding());
            if (vec != null) {
                localCache.put(id, vec);
                return vec;
            }
        }
        return null;
    }

    /** Get all cached embeddings (local fallback path). */
    public Map<Long, float[]> getAll(List<Long> ids) {
        Map<Long, float[]> result = new HashMap<>();
        if (redisAvailable) {
            try {
                List<String> keys = ids.stream().map(id -> EMB_PREFIX + id).toList();
                List<String> values = redisTemplate.opsForValue().multiGet(keys);
                if (values != null) {
                    for (int i = 0; i < ids.size(); i++) {
                        String json = values.get(i);
                        if (json != null) {
                            float[] vec = embeddingService.deserialize(json);
                            if (vec != null) result.put(ids.get(i), vec);
                        }
                    }
                }
                // Check if we got everything
                if (!ids.isEmpty()) {
                    long hitCount = result.size();
                    if (hitCount == 0 && ids.size() > 0) {
                        // Possible Redis issue, fall back
                        throw new RuntimeException("Redis returned no results");
                    }
                }
                return result;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis batch read failed, switching to local cache");
                loadAllToLocal();
            }
        }
        for (Long id : ids) {
            float[] vec = get(id);
            if (vec != null) result.put(id, vec);
        }
        return result;
    }

    /** Remove embedding from cache. */
    public void remove(Long id) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(EMB_PREFIX + id);
            } catch (Exception ignored) {}
        }
        localCache.remove(id);
    }

    /** Store dedup hash → chunk ids mapping. */
    public void putHashMapping(String sha256, List<Long> chunkIds) {
        if (redisAvailable) {
            try {
                String val = chunkIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
                redisTemplate.opsForValue().set(HASH_PREFIX + sha256, val, TTL);
            } catch (Exception ignored) {}
        }
    }

    /** Get existing chunk ids for a source hash (for dedup). */
    public List<Long> getHashMapping(String sha256) {
        if (redisAvailable) {
            try {
                String val = redisTemplate.opsForValue().get(HASH_PREFIX + sha256);
                if (val != null && !val.isEmpty()) {
                    return Arrays.stream(val.split(",")).map(Long::parseLong).toList();
                }
            } catch (Exception ignored) {}
        }
        return Collections.emptyList();
    }

    /** Remove hash mapping. */
    public void removeHashMapping(String sha256) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(HASH_PREFIX + sha256);
            } catch (Exception ignored) {}
        }
    }

    private void loadAllToLocal() {
        try {
            List<KnowledgeBase> all = knowledgeBaseMapper.selectList(null);
            for (KnowledgeBase kb : all) {
                float[] vec = embeddingService.deserialize(kb.getEmbedding());
                if (vec != null) localCache.put(kb.getId(), vec);
            }
            log.info("Loaded {} embeddings into local cache", localCache.size());
        } catch (Exception e) {
            log.error("Failed to load embeddings to local cache", e);
        }
    }
}