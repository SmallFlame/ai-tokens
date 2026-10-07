package com.aitoken.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aitoken.common.Result;
import com.aitoken.dto.request.CallLogQuery;
import com.aitoken.dto.request.ChangePasswordRequest;
import com.aitoken.dto.response.UserOverviewVO;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.CallLog;
import com.aitoken.entity.User;
import com.aitoken.mapper.CallLogMapper;
import com.aitoken.service.ApiKeyService;
import com.aitoken.service.CallLogService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "我的信息")
@RestController
@RequestMapping("/api/my")
public class MyController {

    @Resource
    private UserService userService;

    @Resource
    private CallLogService callLogService;

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private CallLogMapper callLogMapper;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<User> info() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        user.setPassword(null);
        userService.enrichUser(user);
        return Result.ok(user);
    }

    @Operation(summary = "获取个人用量概览")
    @GetMapping("/overview")
    public Result<UserOverviewVO> overview() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserOverviewVO vo = callLogService.getUserOverview(userId);
        List<ApiKey> keys = apiKeyService.listByUser(userId);
        vo.setApiKeyCount(keys.size());
        User user = userService.getById(userId);
        if (user != null) {
            String roleCode = (String) StpUtil.getSession().get("roleCode");
            BigDecimal mq = user.getMoneyQuota();
            // 超管/导师 或 unlimited(null/-1) → 返回 null，前端展示"不限制"
            boolean unlimited = "super_admin".equals(roleCode) || "advisor".equals(roleCode)
                    || mq == null
                    || mq.compareTo(new BigDecimal("-1")) == 0;
            if (unlimited) {
                vo.setRemainingMoney(null);
            } else {
                // 透支时显示 0，否则显示实际余额
                vo.setRemainingMoney(mq.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : mq);
            }
            vo.setUsedMoney(user.getUsedMoney() != null ? user.getUsedMoney() : BigDecimal.ZERO);
        }
        return Result.ok(vo);
    }

    @Operation(summary = "查询我的调用记录")
    @GetMapping("/logs")
    public Result<Page<CallLog>> logs(CallLogQuery query) {
        // 强制只查当前用户自己的数据
        query.setUserId(StpUtil.getLoginIdAsLong());
        return Result.ok(callLogService.pageQuery(query));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    @Operation(summary = "修改真实姓名")
    @PutMapping("/nickname")
    public Result<Void> updateNickname(@RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String nickname = body.get("nickname");
        userService.lambdaUpdate().eq(User::getId, userId).set(User::getNickname, nickname).update();
        return Result.ok();
    }

    @Operation(summary = "修改邮箱")
    @PutMapping("/email")
    public Result<Void> updateEmail(@RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String email = body.get("email");
        userService.lambdaUpdate().eq(User::getId, userId).set(User::getEmail, email).update();
        return Result.ok();
    }

    @Operation(summary = "各渠道费用分布（饼图）")
    @GetMapping("/chart/channel-cost")
    public Result<List<Map<String, Object>>> channelCost(
            @RequestParam(defaultValue = "1") int days) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(callLogMapper.selectUserChannelCost(userId, days));
    }

    @Operation(summary = "Token/费用趋势（折线/柱图）")
    @GetMapping("/chart/trend")
    public Result<List<Map<String, Object>>> trend(
            @RequestParam(defaultValue = "7") int days) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(callLogMapper.selectUserTrend(userId, days));
    }
}
