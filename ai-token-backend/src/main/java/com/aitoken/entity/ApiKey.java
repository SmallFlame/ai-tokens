package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("t_api_key")
public class ApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** API Key 值, 格式: sk-xxx */
    private String keyValue;

    private String name;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 总 Token 额度, -1 不限制 */
    private Long totalQuota;

    /** 已用 Token 数 */
    private Long usedQuota;

    /** Key 类型: 1-Token额度型(管理员设quota,不扣金额) 2-金额消费型(扣用户余额,默认) */
    private Integer keyType;

    /** 绑定渠道ID，null 表示走负载均衡 */
    private Long channelId;

    /** 过期时间, null 代表永不过期 */
    private Date expiredAt;

    /** 已用金额(元) */
    private java.math.BigDecimal usedMoney;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
