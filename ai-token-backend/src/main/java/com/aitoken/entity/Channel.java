package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("t_channel")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * 渠道类型: openai / azure / wenxin / qianwen / custom
     */
    private String type;

    private String baseUrl;

    /** 渠道真实 API Key (AES 加密存储) */
    private String apiKey;

    /** 支持的模型列表 (JSON 数组字符串) */
    private String models;

    /** 负载均衡权重 */
    private Integer weight;

    /** 优先级, 越大越优先 */
    private Integer priority;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 请求超时(ms) */
    private Integer timeoutMs;

    /** 健康状态: 0-异常, 1-正常 */
    private Integer healthStatus;

    private Date lastCheckAt;

    private String remark;

    /** 渠道总预算（元），管理员充值累加 */
    private java.math.BigDecimal totalMoney;

    /** 渠道已用金额（元），每次代理调用后累加 */
    private java.math.BigDecimal usedMoney;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
