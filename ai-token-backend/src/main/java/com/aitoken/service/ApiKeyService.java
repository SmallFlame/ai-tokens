package com.aitoken.service;

import com.aitoken.dto.request.ApiKeyCreateRequest;
import com.aitoken.dto.request.BatchKeyAssignRequest;
import com.aitoken.entity.ApiKey;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface ApiKeyService extends IService<ApiKey> {

    ApiKey createKey(Long userId, ApiKeyCreateRequest request);

    List<ApiKey> listByUser(Long userId);

    /** 管理员查询全量 Key，可按 userId 过滤 */
    List<ApiKey> listAll(Long userId);

    void revokeKey(Long id);

    /** 管理员修改额度、过期时间、Key类型 */
    void adminUpdateKey(Long id, Long totalQuota, Date expiredAt, Integer keyType);

    /** 管理员修改绑定渠道（null 表示走负载均衡） */
    void adminUpdateChannel(Long id, Long channelId);

    /** 管理员启用/禁用 Key */
    void adminUpdateStatus(Long id, Integer status);

    /**
     * 根据 key 值查询有效的 API Key (代理层使用)
     */
    ApiKey getValidKey(String keyValue);

    /**
     * 原子累加已使用 Token 数
     */
    void addUsedQuota(Long id, long tokens);

    /**
     * 累加 Key 已用金额（统计用）
     */
    void addUsedMoney(Long id, BigDecimal amount);

    /** 管理员批量分配 Token 额度 */
    void batchAssign(BatchKeyAssignRequest request);
}
