package com.aitoken.service;

import com.aitoken.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface RoleService extends IService<Role> {

    /** 查询所有角色 */
    List<Role> listAll();

    /** 根据 role code 查角色 */
    Role getByCode(String code);
}
