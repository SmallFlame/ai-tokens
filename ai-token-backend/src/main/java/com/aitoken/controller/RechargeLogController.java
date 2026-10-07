package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.entity.GroupRechargeLog;
import com.aitoken.entity.UserRechargeLog;
import com.aitoken.mapper.GroupRechargeLogMapper;
import com.aitoken.mapper.UserRechargeLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "充值日志查询")
@RestController
@RequestMapping("/api/admin/recharge-logs")
public class RechargeLogController {

    @Resource
    private UserRechargeLogMapper userRechargeLogMapper;

    @Resource
    private GroupRechargeLogMapper groupRechargeLogMapper;

    // ── 个人充值日志 ──

    @Operation(summary = "分页查询个人充值日志")
    @GetMapping("/user")
    public Result<Page<UserRechargeLog>> listUserLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<UserRechargeLog> wrapper = new LambdaQueryWrapper<UserRechargeLog>()
                .eq(userId != null, UserRechargeLog::getUserId, userId)
                .like(username != null && !username.isEmpty(), UserRechargeLog::getUsername, username)
                .eq(type != null && !type.isEmpty(), UserRechargeLog::getType, type)
                .orderByDesc(UserRechargeLog::getCreatedAt);
        return Result.ok(userRechargeLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    // ── 小组充值日志 ──

    @Operation(summary = "分页查询小组充值日志")
    @GetMapping("/group")
    public Result<Page<GroupRechargeLog>> listGroupLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<GroupRechargeLog> wrapper = new LambdaQueryWrapper<GroupRechargeLog>()
                .eq(groupId != null, GroupRechargeLog::getGroupId, groupId)
                .like(groupName != null && !groupName.isEmpty(), GroupRechargeLog::getGroupName, groupName)
                .eq(type != null && !type.isEmpty(), GroupRechargeLog::getType, type)
                .orderByDesc(GroupRechargeLog::getCreatedAt);
        return Result.ok(groupRechargeLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }
}
