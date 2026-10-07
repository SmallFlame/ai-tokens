package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.common.exception.BizException;
import com.aitoken.dto.request.UserCreateRequest;
import com.aitoken.dto.response.DuplicateGroupVO;
import com.aitoken.entity.Role;
import com.aitoken.entity.User;
import com.aitoken.service.RoleService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 导师专用用户管理：可筛选组内/组外，改角色后自动设为组外
 */
@Tag(name = "用户管理（导师）")
@RestController
@RequestMapping("/api/admin/admin-users")
public class AdminUserMgmtController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Operation(summary = "分页查询用户（groupStatus: null=全部, 0=组外, 1=组内）")
    @GetMapping
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1")  int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false)    String username,
            @RequestParam(required = false)    Integer groupStatus) {
        Page<User> page = userService.pageOutGroupUsers(pageNum, pageSize, username, groupStatus);
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(page);
    }

    @Operation(summary = "创建用户（角色仅组长或组员，默认组外）")
    @PostMapping
    public Result<User> create(@RequestBody UserCreateRequest request) {
        Long roleId = request.getRoleId() == null ? roleService.getByCode("member").getId() : request.getRoleId();
        // 仅允许创建组长或组员
        Role role = roleService.getById(roleId);
        if (role == null || (!"leader".equals(role.getCode()) && !"member".equals(role.getCode()))) {
            throw BizException.of("只能创建组长或组员");
        }
        request.setRoleId(roleId);
        User user = userService.createUser(request);
        user.setPassword(null);
        return Result.ok(user);
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @Operation(summary = "给组外用户充值/扣减余额（delta>0 充值，<0 扣减）")
    @PutMapping("/{id}/recharge-money")
    public Result<Void> rechargeMoney(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body.get("delta") == null) throw BizException.of("缺少 delta 参数");
        BigDecimal delta = new BigDecimal(body.get("delta").toString());
        userService.rechargeUserMoney(id, delta);
        return Result.ok();
    }

    @Operation(summary = "设置用户角色（仅组长或组员），同时将用户设为组外")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long roleId = Long.valueOf(body.get("roleId").toString());
        Role role = roleService.getById(roleId);
        if (role == null || (!"leader".equals(role.getCode()) && !"member".equals(role.getCode()))) {
            throw BizException.of("只能设置为组长或组员");
        }
        userService.updateRoleId(id, roleId);
        userService.setGroupStatus(id, 0);
        return Result.ok();
    }

    @Operation(summary = "查找重复账号（学号或邮箱相同）")
    @GetMapping("/duplicates")
    public Result<List<DuplicateGroupVO>> duplicates(
            @RequestParam(required = false) String keyword) {
        return Result.ok(userService.findDuplicateUsers(keyword));
    }

    @Operation(summary = "分页查询已冻结账号")
    @GetMapping("/deleted")
    public Result<Page<User>> deleted(
            @RequestParam(defaultValue = "1")  int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false)    String username) {
        Page<User> page = userService.pageDeletedUsers(pageNum, pageSize, username);
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(page);
    }

    @Operation(summary = "解冻账号")
    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        userService.restoreUser(id);
        return Result.ok();
    }
}
