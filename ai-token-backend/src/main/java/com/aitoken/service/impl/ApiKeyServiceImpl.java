package com.aitoken.service.impl;

import com.aitoken.common.exception.BizException;
import com.aitoken.dto.request.ApiKeyCreateRequest;
import com.aitoken.dto.request.BatchKeyAssignRequest;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.User;
import com.aitoken.mapper.ApiKeyMapper;
import com.aitoken.service.ApiKeyService;
import com.aitoken.service.UserService;
import com.aitoken.util.ApiKeyUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey> implements ApiKeyService {

    @Resource
    private ApiKeyUtil apiKeyUtil;

    @Resource
    private UserService userService;

    @Override
    public ApiKey createKey(Long userId, ApiKeyCreateRequest request) {
        ApiKey apiKey = new ApiKey();
        apiKey.setUserId(userId);
        apiKey.setKeyValue(apiKeyUtil.generate());
        apiKey.setName(request.getName());
        apiKey.setChannelId(request.getChannelId());
        apiKey.setStatus(1);
        apiKey.setKeyType(request.getKeyType() == null ? 2 : request.getKeyType());
        apiKey.setTotalQuota(request.getTotalQuota() == null ? -1L : request.getTotalQuota());
        apiKey.setUsedQuota(0L);
        apiKey.setUsedMoney(BigDecimal.ZERO);
        apiKey.setExpiredAt(request.getExpiredAt());
        save(apiKey);
        return apiKey;
    }

    @Override
    public List<ApiKey> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreatedAt));
    }

    @Override
    public List<ApiKey> listAll(Long userId) {
        return list(new LambdaQueryWrapper<ApiKey>()
                .eq(userId != null, ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreatedAt));
    }

    @Override
    public void adminUpdateKey(Long id, Long totalQuota, Date expiredAt, Integer keyType) {
        ApiKey key = getById(id);
        if (key == null) throw BizException.of("API Key 不存在");
        lambdaUpdate()
                .eq(ApiKey::getId, id)
                .set(totalQuota != null, ApiKey::getTotalQuota, totalQuota)
                .set(ApiKey::getExpiredAt, expiredAt)
                .set(keyType != null, ApiKey::getKeyType, keyType)
                .update();
    }

    @Override
    public void adminUpdateChannel(Long id, Long channelId) {
        lambdaUpdate().eq(ApiKey::getId, id).set(ApiKey::getChannelId, channelId).update();
    }

    @Override
    public void adminUpdateStatus(Long id, Integer status) {
        lambdaUpdate().eq(ApiKey::getId, id).set(ApiKey::getStatus, status).update();
    }

    @Override
    public void revokeKey(Long id) {
        ApiKey key = getById(id);
        if (key == null) throw BizException.of("API Key 不存在");
        key.setStatus(0);
        updateById(key);
    }

    @Override
    public ApiKey getValidKey(String keyValue) {
        ApiKey key = getOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getKeyValue, keyValue)
                .eq(ApiKey::getStatus, 1));
        if (key == null) {
            throw BizException.of(401, "API Key 无效或已禁用");
        }
        // 属主账号被冻结时 Key 立即失效
        User owner = userService.getById(key.getUserId());
        if (owner == null) {
            throw BizException.of(401, "账号已冻结，API Key 失效");
        }
        if (key.getExpiredAt() != null && key.getExpiredAt().before(new Date())) {
            throw BizException.of(401, "API Key 已过期");
        }
        // 仅 Token额度型(keyType=1) 做 Token 配额检查
        if (Integer.valueOf(1).equals(key.getKeyType())
                && key.getTotalQuota() != -1
                && key.getUsedQuota() >= key.getTotalQuota()) {
            throw BizException.of(429, "Token 额度已耗尽");
        }
        return key;
    }

    @Override
    public void addUsedQuota(Long id, long tokens) {
        baseMapper.addUsedQuota(id, tokens);
    }

    @Override
    public void addUsedMoney(Long id, BigDecimal amount) {
        baseMapper.addUsedMoney(id, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssign(BatchKeyAssignRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw BizException.of("请选择至少一个用户");
        }
        ApiKeyCreateRequest createReq = new ApiKeyCreateRequest();
        createReq.setName(request.getName());
        createReq.setTotalQuota(request.getTotalQuota());
        createReq.setExpiredAt(request.getExpiredAt());

        for (Long userId : request.getUserIds()) {
            if ("update".equals(request.getMode())) {
                List<ApiKey> keys = listByUser(userId);
                if (!keys.isEmpty()) {
                    ApiKey latest = keys.get(0);
                    long existing = latest.getTotalQuota() == null ? 0L : latest.getTotalQuota();
                    long newQuota = existing == -1L ? -1L : existing + request.getTotalQuota();
                    lambdaUpdate()
                            .eq(ApiKey::getId, latest.getId())
                            .set(ApiKey::getTotalQuota, newQuota)
                            .set(ApiKey::getExpiredAt, request.getExpiredAt())
                            .update();
                } else {
                    createKey(userId, createReq);
                }
            } else {
                createKey(userId, createReq);
            }
        }
    }
}
