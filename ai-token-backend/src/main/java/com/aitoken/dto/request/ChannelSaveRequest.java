package com.aitoken.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ChannelSaveRequest {

    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** openai / azure / wenxin / qianwen / custom */
    @NotBlank(message = "渠道类型不能为空")
    private String type;

    @NotBlank(message = "Base URL 不能为空")
    private String baseUrl;

    @NotBlank(message = "API Key 不能为空")
    private String apiKey;

    /** 支持的模型列表 */
    private List<String> models;

    private Integer weight = 1;

    private Integer priority = 0;

    /** 请求超时(ms), 默认 5 分钟（兼容 Opus 等响应较慢的模型） */
    private Integer timeoutMs = 300000;

    private String remark;
}
