package com.aitoken.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aitoken.common.Result;
import com.aitoken.dto.request.ApiKeyCreateRequest;
import com.aitoken.dto.request.UserCreateRequest;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.Group;
import com.aitoken.entity.User;
import com.aitoken.service.GroupService;
import com.aitoken.service.TeamService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组长端接口（/api/team/**，需要 "leader" 角色）
 */
@Tag(name = "团队管理")
@RestController
@RequestMapping("/api/team")
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private GroupService groupService;

    @Resource
    private UserService userService;

    // ── 成员管理 ──

    @Operation(summary = "查看我的团队成员列表")
    @GetMapping("/members")
    public Result<List<User>> listMembers() {
        Long leaderId = StpUtil.getLoginIdAsLong();
        List<User> members = teamService.listMembers(leaderId);
        members.forEach(u -> u.setPassword(null));
        return Result.ok(members);
    }

    @Operation(summary = "创建团队组员")
    @PostMapping("/members")
    public Result<User> createMember(@RequestBody UserCreateRequest request) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        return Result.ok(teamService.createMember(leaderId, request));
    }

    @Operation(summary = "启用/禁用组员")
    @PutMapping("/members/{id}/status")
    public Result<Void> updateMemberStatus(@PathVariable Long id, @RequestParam Integer status) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        teamService.updateMemberStatus(leaderId, id, status);
        return Result.ok();
    }

    @Operation(summary = "删除组员")
    @DeleteMapping("/members/{id}")
    public Result<Void> deleteMember(@PathVariable Long id) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        teamService.deleteMember(leaderId, id);
        return Result.ok();
    }

    // ── Key 管理 ──

    @Operation(summary = "查看团队所有 API Key（含自己和组员）")
    @GetMapping("/keys")
    public Result<List<ApiKey>> listKeys() {
        Long leaderId = StpUtil.getLoginIdAsLong();
        return Result.ok(teamService.listTeamKeys(leaderId));
    }

    @Operation(summary = "为组员（或自己）创建 API Key")
    @PostMapping("/keys")
    public Result<ApiKey> createKey(@RequestParam Long targetUserId,
                                     @RequestBody ApiKeyCreateRequest request) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        return Result.ok(teamService.createKey(leaderId, targetUserId, request));
    }

    @Operation(summary = "吊销 Key")
    @DeleteMapping("/keys/{id}")
    public Result<Void> revokeKey(@PathVariable Long id) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        teamService.revokeKey(leaderId, id);
        return Result.ok();
    }

    // ── 额度查询 ──

    @Operation(summary = "查看我的剩余可分配额度和金额池")
    @GetMapping("/quota")
    public Result<Map<String, Object>> myQuota() {
        Long leaderId = StpUtil.getLoginIdAsLong();
        User me = userService.getById(leaderId);
        Map<String, Object> result = new HashMap<>();
        result.put("allocatableQuota", me.getAllocatableQuota() == null ? 0L : me.getAllocatableQuota());

        // 从小组获取金额池
        List<Group> groups = groupService.list(new LambdaQueryWrapper<Group>().eq(Group::getLeaderId, leaderId));
        BigDecimal moneyPool = BigDecimal.ZERO;
        if (!groups.isEmpty()) {
            moneyPool = groups.get(0).getMoneyPool();
            if (moneyPool == null) moneyPool = BigDecimal.ZERO;
        }
        result.put("allocatableMoney", moneyPool);
        return Result.ok(result);
    }

    // ── 拉人 / 释放 / 金额分配 ──

    @Operation(summary = "获取可被拉入的用户（未分组的组员）")
    @GetMapping("/available-members")
    public Result<List<User>> availableMembers() {
        List<User> users = teamService.getAvailableUsers();
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    @Operation(summary = "将已有用户拉入我的小组")
    @PostMapping("/pull/{userId}")
    public Result<Void> pullMember(@PathVariable Long userId) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        teamService.pullMember(leaderId, userId);
        return Result.ok();
    }

    @Operation(summary = "将组员移出小组（不删除账号），归还剩余金额到组长池")
    @DeleteMapping("/release/{userId}")
    public Result<Void> releaseMember(@PathVariable Long userId) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        teamService.releaseMember(leaderId, userId);
        return Result.ok();
    }

    @Operation(summary = "调整组员余额（delta>0新增，delta<0减少；自动与组长金额池互转）")
    @PutMapping("/members/{id}/adjust-money")
    public Result<Void> adjustMemberMoney(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        Long leaderId = StpUtil.getLoginIdAsLong();
        BigDecimal delta = new BigDecimal(body.get("delta").toString());
        teamService.adjustMemberMoney(leaderId, id, delta);
        return Result.ok();
    }
}
