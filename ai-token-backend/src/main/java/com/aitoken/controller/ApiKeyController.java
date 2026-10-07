package com.aitoken.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckDisable;
import com.aitoken.common.Result;
import com.aitoken.common.exception.BizException;
import com.aitoken.dto.request.ApiKeyCreateRequest;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.User;
import com.aitoken.service.ApiKeyService;
import com.aitoken.service.RoleService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.List;

@Tag(name = "API Key 管理")
@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Operation(summary = "创建 API Key" +
            "（所有登录用户可用，Token额度型仅 super_admin 可创建）")
    @PostMapping
    public Result<ApiKey> create(@RequestBody ApiKeyCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        String roleCode = (String) StpUtil.getSession().get("roleCode");
        // 只有 super_admin 才能创建 Token额度型 Key type=1 tokens type=2是金额
        Integer keyType = request.getKeyType() == null ?
                2 : request.getKeyType();
        if (keyType == 1 && !"super_admin".equals(roleCode)) {
            throw BizException.of(403, "Token额度型" +
                    "密钥只有超管可以创建");
        }
        // 非 super_admin 强制为金额消费型
        if (!"super_admin".equals(roleCode)) {
            request.setKeyType(2);
        }
        // 用户门户创建时默认无额度，需管理员手动分配；7 天有效期
        if (request.getTotalQuota() == null
                || request.getTotalQuota() == -1L) {
            request.setTotalQuota(0L);
        }
        if (request.getExpiredAt() == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 7);
            request.setExpiredAt(cal.getTime());
        }
        return Result.ok(apiKeyService.createKey(userId, request));
    }

    @Operation(summary = "查询我的 API Key 列表")
    @GetMapping
    public Result<List<ApiKey>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(apiKeyService.listByUser(userId));
    }
//    @Operation(summary = "查询我的 API Key 列表qt")
//    @SaCheckDisable
//    @GetMapping("/qt/{username}")
//    public Result<List<ApiKey>> listqt(@PathVariable String username) {
//
//        QueryWrapper q = new QueryWrapper();
//        q.eq("username",username);
//        User user = userService.getOne(q);
//        QueryWrapper q1 = new QueryWrapper();
//        q1.eq("user_id",user.getId());
//        q1.orderByDesc("created_at");
//        return Result.ok(apiKeyService.list(q1));
//    }
    @Operation(summary = "吊销 API Key")
    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        apiKeyService.revokeKey(id);
        return Result.ok();
    }

    @Operation(summary = "修改绑定渠道（null 清除绑定，走自动负载均衡）")
    @PutMapping("/{id}/channel")
    public Result<Void> updateChannel(@PathVariable Long id,
                                       @RequestParam(required = false) Long channelId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ApiKey key = apiKeyService.getById(id);
        if (key == null || !key.getUserId().equals(userId)) {
            throw BizException.of(403, "无权操作");
        }
        apiKeyService.adminUpdateChannel(id, channelId);
        return Result.ok();
    }
}
