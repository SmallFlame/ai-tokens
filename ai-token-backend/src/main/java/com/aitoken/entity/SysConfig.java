package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("t_sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String configKey;

    private String configValue;

    private String description;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
