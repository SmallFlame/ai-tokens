package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("t_role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
