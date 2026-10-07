package com.aitoken.config;

import cn.dev33.satoken.stp.StpInterface;
import com.aitoken.entity.Role;
import com.aitoken.entity.User;
import com.aitoken.mapper.RoleMapper;
import com.aitoken.mapper.UserMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 自定义权限接口实现
 * 登录时存储 userId, 框架调用此类获取角色/权限列表
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }
        Long roleId = user.getRoleId();
        if (roleId == null) {
            return Collections.singletonList("member");
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return Collections.singletonList("member");
        }
        // 直接返回 t_role.code 作为 Sa-Token 角色标识
        List<String> roles = new ArrayList<>();
        roles.add(role.getCode());
        // super_admin 自动拥有 leader 和 advisor 的所有权限
        if ("super_admin".equals(role.getCode())) {
            roles.add("advisor");
            roles.add("leader");
        }
        // advisor 自动拥有 leader 的权限
        if ("advisor".equals(role.getCode())) {
            roles.add("leader");
        }
        return roles;
    }
}
