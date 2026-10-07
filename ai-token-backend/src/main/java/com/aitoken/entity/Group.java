package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_group")
public class Group {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String remark;

    private Long leaderId;

    private BigDecimal moneyPool;

    /** 学期标识 */
    private String semester;

    /** 周报类型: 1-需交周报(默认) 0-不需交 */
    private Integer reportType;

    /** 创建者用户ID */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
