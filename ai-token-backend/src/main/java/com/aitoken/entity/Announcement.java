package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("t_announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容（支持 HTML） */
    private String content;

    /** 发布人 */
    private String publisher;

    /** 是否显示在主页面: 0-否, 1-是（全局唯一） */
    private Integer showOnHome;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
