package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_user_recharge_log")
public class UserRechargeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private Long operatorId;

    private String operatorName;

    /** 类型: recharge=充值, set_balance=设置余额, batch_set=批量设置, assign_pool=分配组长池, member_adjust=组长调余额 */
    private String type;

    private BigDecimal amount;

    private BigDecimal beforeBalance;

    private BigDecimal afterBalance;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
