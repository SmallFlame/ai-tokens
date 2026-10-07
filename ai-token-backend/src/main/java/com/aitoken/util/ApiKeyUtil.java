package com.aitoken.util;

import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 平台 API Key 生成工具
 */
@Component
public class ApiKeyUtil {

    @Value("${aitoken.api-key-prefix:sk-}")
    private String prefix;

    /**
     * 生成平台 API Key, 格式: sk-{32位随机串}
     */
    public String generate() {
        // 去除 UUID 中的 '-', 得到 32 位随机串
        return prefix + IdUtil.simpleUUID();
    }
}
