package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.entity.Role;
import com.aitoken.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {

    @Resource
    private RoleService roleService;

    @Operation(summary = "查询所有角色")
    @GetMapping
    public Result<List<Role>> listAll() {
        return Result.ok(roleService.listAll());
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public Result<Role> create(@RequestBody Role role) {
        roleService.save(role);
        return Result.ok(role);
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateById(role);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.ok();
    }
}
