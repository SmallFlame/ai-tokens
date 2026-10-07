package com.aitoken.dto.request;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String nickname;
    private String password;
    private String email;
    /** 角色ID，关联 t_role.id；默认 4（组员） */
    private Long roleId = 4L;
    /** 上级组长用户ID，组员必填（管理员创建时使用） */
    private Long parentId;
    /** 学号/工号（选填） */
    private String studentId;
}
