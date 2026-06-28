package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.file.domain.config.SysConfig;
import com.qiwenshare.file.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper configMapper;

    public Map<String, String> getAllConfig() {
        List<SysConfig> list = configMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        list.forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }

    public void saveConfig(Map<String, String> config) {
        config.forEach((key, value) -> {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getConfigKey, key);
            SysConfig exist = configMapper.selectOne(wrapper);
            if (exist != null) {
                exist.setConfigValue(value);
                configMapper.updateById(exist);
            } else {
                SysConfig cfg = new SysConfig();
                cfg.setConfigKey(key);
                cfg.setConfigValue(value);
                cfg.setDescription(key);
                configMapper.insert(cfg);
            }
        });
    }
}
