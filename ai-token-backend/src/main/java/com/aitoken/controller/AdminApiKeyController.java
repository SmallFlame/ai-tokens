package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.dto.request.ApiKeyCreateRequest;
import com.aitoken.dto.request.BatchKeyAssignRequest;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.User;
import com.aitoken.service.ApiKeyService;
import com.aitoken.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "管理端 - API Key 管理")
@RestController
@RequestMapping("/api/admin/keys")
public class AdminApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private UserService userService;

    @Operation(summary = "查询全量 API Key（含用户名）")
    @GetMapping
    public Result<List<Map<String, Object>>> listAll(
            @RequestParam(required = false) Long userId) {

        List<ApiKey> keys = apiKeyService.listAll(userId);

        // 批量查用户名
        Set<Long> userIds = keys.stream().map(ApiKey::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernameMap = userIds.isEmpty() ? Collections.emptyMap() :
                userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));

        List<Map<String, Object>> result = keys.stream().map(k -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",         k.getId());
            row.put("userId",     k.getUserId());
            row.put("username",   usernameMap.getOrDefault(k.getUserId(), "-"));
            row.put("keyValue",   k.getKeyValue());
            row.put("name",       k.getName());
            row.put("status",     k.getStatus());
            row.put("keyType",    k.getKeyType());
            row.put("totalQuota", k.getTotalQuota());
            row.put("usedQuota",  k.getUsedQuota());
            row.put("usedMoney",  k.getUsedMoney());
            row.put("expiredAt",  k.getExpiredAt());
            row.put("createdAt",  k.getCreatedAt());
            row.put("channelId",  k.getChannelId());
            return row;
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    @Operation(summary = "为指定用户创建 API Key")
    @PostMapping
    public Result<ApiKey> create(@RequestParam Long userId,
                                  @RequestBody ApiKeyCreateRequest request) {
        return Result.ok(apiKeyService.createKey(userId, request));
    }

    @Operation(summary = "修改绑定渠道（null 表示走负载均衡）")
    @PutMapping("/{id}/channel")
    public Result<Void> updateChannel(@PathVariable Long id,
                                       @RequestParam(required = false) Long channelId) {
        apiKeyService.adminUpdateChannel(id, channelId);
        return Result.ok();
    }

    @Operation(summary = "修改额度、过期时间、Key类型")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                                @RequestBody ApiKeyCreateRequest request) {
        apiKeyService.adminUpdateKey(id, request.getTotalQuota(), request.getExpiredAt(), request.getKeyType());
        return Result.ok();
    }

    @Operation(summary = "启用 / 禁用 API Key")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                      @RequestParam Integer status) {
        apiKeyService.adminUpdateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "吊销 API Key")
    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        apiKeyService.revokeKey(id);
        return Result.ok();
    }

    @Operation(summary = "批量为用户分配 Token 额度")
    @PostMapping("/batch-assign")
    public Result<Void> batchAssign(@RequestBody BatchKeyAssignRequest request) {
        apiKeyService.batchAssign(request);
        return Result.ok();
    }
}
