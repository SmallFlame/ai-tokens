package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/** 避免与 java.lang.reflect.Method 等系统类冲突, 实体类命名为 AiModel */
@Data
@TableName("t_model")
public class AiModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    /** 真实模型名, 如 gpt-4o */
    private String modelName;

    /** 别名, 用户请求时使用 */
    private String alias;

    /** 输入 Token 单价(元/百万Token) */
    private BigDecimal inputPrice;

    /** 输出 Token 单价(元/百万Token) */
    private BigDecimal outputPrice;

    /** 缓存创建单价(元/百万Token)，null 时等于 inputPrice */
    private BigDecimal cacheCreationPrice;

    /** 缓存命中单价(元/百万Token)，null 时等于 inputPrice × 0.1 */
    private BigDecimal cacheReadPrice;

    /** 最大输出 Token 数 */
    private Integer maxTokens;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 权重, 用于同模型多渠道加权随机路由, 默认 1 */
    private Integer weight;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
