package com.aitoken.dto.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    /** 角色编码: super_admin/advisor/leader/member */
    private String roleCode;
    /** 角色显示名: 超级管理员/导师/组长/组员 */
    private String roleName;
    /** 所属小组ID */
    private Long groupId;
    /** 所属小组名称 */
    private String groupName;
    /** 学号/工号 */
    private String studentId;
    /** 头像URL */
    private String avatar;
    /** 邮箱 */
    private String email;
}
