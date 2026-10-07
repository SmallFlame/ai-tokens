package com.aitoken.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 面向用户的渠道信息（不含 API Key / Base URL 等敏感字段）
 */
@Data
public class ChannelInfoVO {

    private Long channelId;
    private String channelName;

    /** 该渠道支持的模型列表（仅启用状态的模型） */
    private List<ModelItem> models;

    @Data
    public static class ModelItem {
        private Long modelId;
        /** 真实模型名，如 claude-sonnet-4-6 */
        private String modelName;
        /** 用户请求时使用的别名（为空时直接用 modelName） */
        private String alias;
        /** 输入 Token 单价（元 / 千 Token） */
        private BigDecimal inputPrice;
        /** 输出 Token 单价（元 / 千 Token） */
        private BigDecimal outputPrice;
        /** 最大输出 Token 数 */
        private Integer maxTokens;
    }
}
