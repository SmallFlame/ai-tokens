package com.aitoken.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 真实姓名 */
    private String nickname;

    private String password;

    private String email;

    /** 角色: 0-超级管理员, 1-组长, 2-组员, 3-管理员 (已废弃，请使用 roleId) */
    @Deprecated
    private Integer role;

    /** 角色ID，关联 t_role.id */
    private Long roleId;

    // ── 以下为瞬态字段，不持久化到数据库 ──

    @TableField(exist = false)
    private String groupName;       // 所属小组名称

    @TableField(exist = false)
    private String parentName;      // 上级（组长）姓名

    @TableField(exist = false)
    private String roleName;        // 角色名称

    /** 学号/工号 */
    private String studentId;

    /** 头像URL */
    private String avatar;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 组状态: 0-组外, 1-组内 */
    private Integer groupStatus;

    /** 上级用户ID（组员→组长），管理员和组长为 null */
    private Long parentId;

    /** 所属小组ID */
    private Long groupId;

    /** 可分配 Token 额度（组长专用，由管理员充值，组长分给组员时扣减） */
    private Long allocatableQuota;

    /** 可分配金额池（组长专用，由管理员充值，组长分给组员时扣减） */
    private java.math.BigDecimal allocatableMoney;

    /** 金额余额（元） */
    private BigDecimal moneyQuota;

    /** 已用金额（元） */
    private BigDecimal usedMoney;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 逻辑删除: 0-正常 1-已冻结 */
    @TableLogic
    private Integer deleted;
}
