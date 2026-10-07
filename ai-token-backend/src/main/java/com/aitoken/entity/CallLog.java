package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_call_log")
public class CallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long apiKeyId;

    private Long channelId;

    /** 渠道名称(冗余字段, 避免关联查询) */
    private String channelName;

    /** 请求模型名(用户填写) */
    private String modelName;

    /** 实际转发的模型名 */
    private String realModel;

    /** 请求唯一 ID (UUID) */
    private String requestId;

    /** 客户端 IP */
    private String clientIp;

    /** 输入 Token 数（非缓存部分） */
    private Integer promptTokens;

    /** 输出 Token 数 */
    private Integer completionTokens;

    /** 缓存创建 Token 数（Anthropic prompt cache） */
    private Integer cacheCreationTokens;

    /** 缓存命中 Token 数（Anthropic prompt cache） */
    private Integer cacheReadTokens;

    /** 总 Token 数（含缓存） */
    private Integer totalTokens;

    /** 费用估算(元) */
    private BigDecimal cost;

    /** 请求耗时(ms) */
    private Integer durationMs;

    /** HTTP 状态码 */
    private Integer httpStatus;

    /** 是否流式: 0-否, 1-是 */
    private Integer isStream;

    /** 调用状态: 0-失败, 1-成功 */
    private Integer status;

    private String errorMsg;

    /** 用户名(非持久化字段，查询时补充) */
    @TableField(exist = false)
    private String userName;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
