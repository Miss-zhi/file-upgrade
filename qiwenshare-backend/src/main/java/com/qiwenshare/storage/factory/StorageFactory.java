package com.qiwenshare.storage.factory;

import com.qiwenshare.storage.config.StorageProperties;
import com.qiwenshare.storage.interfaces.StorageBackend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 存储后端工厂。
 *
 * <p>根据 {@code storage.type} 配置返回对应的存储后端实现。
 * 所有 {@link StorageBackend} 实现通过 Spring 依赖注入自动收集。</p>
 */
@Component
@Slf4j
public class StorageFactory {

    private final Map<String, StorageBackend> backendMap;
    private final String activeType;

    /**
     * 构造存储工厂，收集所有 StorageBackend 实现。
     *
     * @param backends        所有存储后端实现
     * @param storageProperties 存储配置
     */
    public StorageFactory(List<StorageBackend> backends, StorageProperties storageProperties) {
        this.backendMap = backends.stream()
                .collect(Collectors.toMap(StorageBackend::getStorageType, Function.identity()));
        this.activeType = storageProperties.getType();
        log.info("存储工厂初始化完成，激活后端: {}，已注册后端: {}", activeType, backendMap.keySet());
    }

    /**
     * 获取当前激活的存储后端。
     *
     * @return 当前激活的 StorageBackend 实现
     * @throws IllegalStateException 如果配置的后端未注册
     */
    public StorageBackend getBackend() {
        StorageBackend backend = backendMap.get(activeType);
        if (backend == null) {
            throw new IllegalStateException("未找到存储后端实现: " + activeType
                    + "，已注册: " + backendMap.keySet());
        }
        return backend;
    }

    /**
     * 根据类型获取指定存储后端。
     *
     * @param type 存储后端类型
     * @return 指定的 StorageBackend 实现
     */
    public StorageBackend getBackend(String type) {
        StorageBackend backend = backendMap.get(type);
        if (backend == null) {
            throw new IllegalStateException("未找到存储后端实现: " + type);
        }
        return backend;
    }

    /**
     * 获取当前激活的存储类型。
     *
     * @return 存储类型字符串
     */
    public String getActiveType() {
        return activeType;
    }

    /**
     * 根据文件存储类型路由到对应后端。
     *
     * <p>用于按文件级 storageType 路由场景：全局切换到新后端后，
     * 旧文件仍可通过文件级 storageType 正确访问。
     * 当目标后端未注册时，fallback 到全局活跃后端并记录 warn 日志。</p>
     *
     * @param storageType 文件的存储类型，可为 null
     * @return 对应的 StorageBackend 实现
     */
    public StorageBackend getBackendForStorageType(String storageType) {
        if (storageType == null) {
            return getBackend();
        }
        StorageBackend backend = backendMap.get(storageType);
        if (backend != null) {
            return backend;
        }
        log.warn("存储后端 [{}] 未注册，fallback 到全局活跃后端 [{}]", storageType, activeType);
        return getBackend();
    }
}
