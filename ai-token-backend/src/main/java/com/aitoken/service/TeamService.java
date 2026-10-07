package com.aitoken.service;

import com.aitoken.dto.request.ApiKeyCreateRequest;
import com.aitoken.dto.request.UserCreateRequest;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.User;

import java.math.BigDecimal;
import java.util.List;

/**
 * 组长端：管理团队成员及成员 API Key
 */
public interface TeamService {

    /** 查询当前组长的所有组员 */
    List<User> listMembers(Long leaderId);

    /** 在当前组长下创建一个组员 */
    User createMember(Long leaderId, UserCreateRequest request);

    /** 启用/禁用组员（只能操作自己团队的成员） */
    void updateMemberStatus(Long leaderId, Long memberId, Integer status);

    /** 删除组员 */
    void deleteMember(Long leaderId, Long memberId);

    /** 列出团队所有 Key（含组长自己的和所有组员的） */
    List<ApiKey> listTeamKeys(Long leaderId);

    /** 为团队成员（或组长自己）创建 Key，从组长 allocatable_quota 中扣减 */
    ApiKey createKey(Long leaderId, Long targetUserId, ApiKeyCreateRequest request);

    /** 修改 Key 的额度，差值计入组长 allocatable_quota */
    void updateKeyQuota(Long leaderId, Long keyId, Long newTotalQuota);

    /** 吊销 Key，将剩余未用额度归还给组长 */
    void revokeKey(Long leaderId, Long keyId);

    /** 获取所有组员（role_code='member'），支持跨组拉入 */
    List<User> getAvailableUsers();

    /** 组长将已有用户拉入自己的小组 */
    void pullMember(Long leaderId, Long userId);

    /** 组长将组员移出小组（不删除账号），归还剩余金额到组长池 */
    void releaseMember(Long leaderId, Long userId);

    /** 组长为组员调整余额（delta>0新增扣减组长池，delta<0减少归还组长池） */
    void adjustMemberMoney(Long leaderId, Long memberId, BigDecimal delta);
}
