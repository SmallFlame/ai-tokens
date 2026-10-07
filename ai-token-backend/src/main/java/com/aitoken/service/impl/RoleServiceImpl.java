package com.aitoken.service.impl;

import com.aitoken.common.exception.BizException;
import com.aitoken.entity.Role;
import com.aitoken.mapper.RoleMapper;
import com.aitoken.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public List<Role> listAll() {
        return list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    @Override
    public Role getByCode(String code) {
        Role role = getOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        if (role == null) {
            throw BizException.of("角色不存在: " + code);
        }
        return role;
    }
}
