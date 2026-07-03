package com.qiwenshare.admin.service;

import com.qiwenshare.admin.common.AdminErrorCode;
import com.qiwenshare.admin.common.AdminModuleException;
import com.qiwenshare.admin.dto.CreateConfigDTO;
import com.qiwenshare.admin.dto.UpdateConfigDTO;
import com.qiwenshare.admin.entity.SystemConfig;
import com.qiwenshare.admin.repository.SystemConfigRepository;
import com.qiwenshare.admin.vo.ConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

/**
 * 系统参数管理服务。
 *
 * <p>提供 CRUD 操作，读取时优先从 Redis 缓存获取（key: {@code sys:config:{configKey}}，TTL 10 分钟）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private static final String CONFIG_CACHE_PREFIX = "sys:config:";
    private static final Duration CONFIG_CACHE_TTL = Duration.ofMinutes(10);

    private final SystemConfigRepository systemConfigRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 分页查询系统参数。
     *
     * @param keyword  搜索关键字（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<ConfigVO> listConfigs(String keyword, Pageable pageable) {
        Page<SystemConfig> page;
        if (keyword != null && !keyword.isBlank()) {
            page = systemConfigRepository.searchByKeyword(keyword, pageable);
        } else {
            page = systemConfigRepository.findAll(pageable);
        }
        return page.map(this::toVO);
    }

    /**
     * 新增系统参数。
     *
     * @param dto 创建请求
     * @return 创建后的 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public ConfigVO createConfig(CreateConfigDTO dto) {
        if (systemConfigRepository.existsByConfigKey(dto.configKey())) {
            throw new AdminModuleException(AdminErrorCode.CONFIG_KEY_DUPLICATE);
        }

        SystemConfig config = new SystemConfig();
        config.setConfigKey(dto.configKey());
        config.setConfigValue(dto.configValue());
        config.setDescription(dto.description());
        systemConfigRepository.save(config);

        log.info("新增系统参数: key={}", dto.configKey());
        return toVO(config);
    }

    /**
     * 修改系统参数。
     *
     * @param id  参数 ID
     * @param dto 更新请求
     * @return 更新后的 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public ConfigVO updateConfig(Long id, UpdateConfigDTO dto) {
        SystemConfig config = systemConfigRepository.findById(id)
                .orElseThrow(() -> new AdminModuleException(AdminErrorCode.CONFIG_NOT_FOUND));

        if (dto.configValue() != null) {
            config.setConfigValue(dto.configValue());
        }
        if (dto.description() != null) {
            config.setDescription(dto.description());
        }
        systemConfigRepository.save(config);

        // 事务提交后再删除缓存，避免事务回滚时缓存已被清除的不一致
        String configKey = config.getConfigKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictCache(configKey);
            }
        });
        log.info("修改系统参数: key={}", configKey);
        return toVO(config);
    }

    /**
     * 删除系统参数。
     *
     * @param id 参数 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SystemConfig config = systemConfigRepository.findById(id)
                .orElseThrow(() -> new AdminModuleException(AdminErrorCode.CONFIG_NOT_FOUND));

        systemConfigRepository.delete(config);
        // 事务提交后再删除缓存
        String configKey = config.getConfigKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictCache(configKey);
            }
        });
        log.info("删除系统参数: key={}", configKey);
    }

    /**
     * 根据 key 获取参数值（供其他模块 Service 调用）。
     * 优先从 Redis 缓存读取，miss 时查 DB 并回填。
     *
     * @param configKey 参数键名
     * @return 参数值，不存在时返回 null
     */
    public String getConfigValue(String configKey) {
        // 先查缓存
        String cached = redisTemplate.opsForValue().get(CONFIG_CACHE_PREFIX + configKey);
        if (cached != null) {
            return cached;
        }

        // 缓存 miss，查 DB
        return systemConfigRepository.findByConfigKey(configKey)
                .map(config -> {
                    redisTemplate.opsForValue().set(CONFIG_CACHE_PREFIX + configKey, config.getConfigValue(), CONFIG_CACHE_TTL);
                    return config.getConfigValue();
                })
                .orElse(null);
    }

    private void evictCache(String configKey) {
        redisTemplate.delete(CONFIG_CACHE_PREFIX + configKey);
    }

    private ConfigVO toVO(SystemConfig config) {
        return new ConfigVO(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription(),
                config.getCreateTime(),
                config.getUpdateTime()
        );
    }
}
