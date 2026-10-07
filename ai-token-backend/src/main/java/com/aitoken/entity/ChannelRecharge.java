package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_channel_recharge")
public class ChannelRecharge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    /** 充值金额（正增加，负减少） */
    private BigDecimal delta;

    private String remark;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
