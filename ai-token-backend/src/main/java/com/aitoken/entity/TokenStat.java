package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_token_stat")
public class TokenStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Date statDate;

    /** -1 = 全天聚合; 0-23 = 对应小时 */
    private Integer statHour;

    /** 0 = 全局 */
    private Long userId;

    /** 0 = 全部渠道 */
    private Long channelId;

    /** 空字符串 = 全部模型 */
    private String modelName;

    private Integer callCount;

    private Integer successCount;

    private Long promptTokens;

    private Long completionTokens;

    private Long totalTokens;

    private BigDecimal totalCost;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
