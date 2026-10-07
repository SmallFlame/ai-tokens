package com.aitoken.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aitoken.common.Result;
import com.aitoken.dto.request.UserCreateRequest;
import com.aitoken.entity.User;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer groupStatus) {
        Page<User> page = userService.pageUsers(pageNum, pageSize, username, status, groupStatus);
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(page);
    }

    @Operation(summary = "创建用户（管理员，支持指定角色和上级组长）")
    @PostMapping
    public Result<User> create(@RequestBody UserCreateRequest request) {
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

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<User> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        user.setPassword(null);
        userService.enrichUser(user);
        return Result.ok(user);
    }

    @Operation(summary = "直接设置用户余额（管理员，可设为任意值）")
    @PutMapping("/{id}/money")
    public Result<Void> updateMoney(@PathVariable Long id,
                                     @RequestParam BigDecimal moneyQuota) {
        userService.adminUpdateMoneyQuota(id, moneyQuota);
        return Result.ok();
    }

    @Operation(summary = "充值/扣减用户余额（delta>0 充值，<0 扣减）")
    @PutMapping("/{id}/recharge-money")
    public Result<Void> rechargeMoney(@PathVariable Long id,
                                       @RequestBody java.util.Map<String, Object> body) {
        if (body.get("delta") == null) return Result.fail("缺少 delta 参数");
        java.math.BigDecimal delta = new java.math.BigDecimal(body.get("delta").toString());
        userService.rechargeUserMoney(id, delta);
        return Result.ok();
    }

    @Operation(summary = "修改用户角色")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id,
                                    @RequestParam Long roleId) {
        userService.updateRoleId(id, roleId);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码为123456")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    @Operation(summary = "批量设置用户金额额度")
    @PostMapping("/batch-money")
    public Result<Void> batchMoney(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("userIds");
        if (ids == null || ids.isEmpty()) return Result.fail("userIds 不能为空");
        List<Long> userIds = ids.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList());
        BigDecimal moneyQuota = new BigDecimal(body.get("moneyQuota").toString());
        userService.batchUpdateMoneyQuota(userIds, moneyQuota);
        return Result.ok();
    }

    @Operation(summary = "给组长充值/扣减可分配金额池（delta>0 增加，<0 减少）")
    @PutMapping("/{id}/allocatable-money")
    public Result<Void> assignAllocatableMoney(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        if (body.get("delta") == null) return Result.fail("缺少 delta 参数");
        BigDecimal delta = new BigDecimal(body.get("delta").toString());
        userService.assignMoneyToLeader(id, delta);
        return Result.ok();
    }
}
