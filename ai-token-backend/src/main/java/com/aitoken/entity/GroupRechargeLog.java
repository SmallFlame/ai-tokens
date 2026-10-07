package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_group_recharge_log")
public class GroupRechargeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private String groupName;

    private Long operatorId;

    private String operatorName;

    /** 类型: recharge=管理员充值, allocate=分给组员, return=组员归还 */
    private String type;

    private BigDecimal amount;

    private BigDecimal beforeBalance;

    private BigDecimal afterBalance;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
