package com.qiwenshare.file.task;

import com.qiwenshare.file.entity.StorageBean;
import com.qiwenshare.file.repository.StorageBeanRepository;
import com.qiwenshare.file.service.StorageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配额同步定时任务。
 *
 * <p>每小时将 Redis 中的实时配额同步到 DB。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaSyncTask {

    private final StorageBeanRepository storageBeanRepository;
    private final StorageQuotaService storageQuotaService;

    /**
     * 每小时执行一次，同步所有用户的 Redis 配额到 DB。
     */
    @Scheduled(fixedRate = 3600000)
    public void syncQuotaToDb() {
        List<StorageBean> allBeans = storageBeanRepository.findAll();
        log.info("开始同步配额: {} 个用户", allBeans.size());

        for (StorageBean bean : allBeans) {
            try {
                storageQuotaService.syncFromDb(bean.getUserId());
            } catch (Exception e) {
                log.error("配额同步失败: userId={}", bean.getUserId(), e);
            }
        }

        log.info("配额同步完成");
    }
}
