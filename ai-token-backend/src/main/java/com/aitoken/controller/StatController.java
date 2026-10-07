package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.dto.response.ChannelStatVO;
import com.aitoken.dto.response.DashboardVO;
import com.aitoken.dto.response.TrendVO;
import com.aitoken.dto.response.UserStatVO;
import com.aitoken.mapper.CallLogMapper;
import com.aitoken.service.StatService;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "统计看板")
@RestController
@RequestMapping("/api/stat")
public class StatController {

    @Resource
    private StatService statService;

    @Resource
    private CallLogMapper callLogMapper;

    @Operation(summary = "获取看板数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        String roleCode = (String) StpUtil.getSession().get("roleCode");
        if ("advisor".equals(roleCode)) {
            return Result.ok(statService.getDashboardForAdmin());
        }
        return Result.ok(statService.getDashboard());
    }

    @Operation(summary = "获取 Token 趋势(折线图)")
    @GetMapping("/trend")
    public Result<List<TrendVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statService.getTrend(days));
    }

    @Operation(summary = "获取各渠道调用统计")
    @GetMapping("/channel")
    public Result<List<ChannelStatVO>> channelStat(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(callLogMapper.selectChannelStat(days));
    }

    @Operation(summary = "获取各用户调用统计")
    @GetMapping("/users")
    public Result<List<UserStatVO>> userStat(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(callLogMapper.selectUserStat(days));
    }
}

