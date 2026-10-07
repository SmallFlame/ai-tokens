package com.aitoken.service;

import com.aitoken.dto.request.LoginRequest;
import com.aitoken.dto.request.RegisterRequest;
import com.aitoken.dto.request.UserCreateRequest;
import com.aitoken.dto.response.LoginResponse;
import com.aitoken.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public interface UserService extends IService<User> {

    LoginResponse login(LoginRequest request);

    User register(RegisterRequest request);

    Page<User> pageUsers(int pageNum, int pageSize, String username, Integer status, Integer groupStatus);

    /** 管理员分页查询用户（groupStatus: null=全部, 0=组外, 1=组内） */
    Page<User> pageOutGroupUsers(int pageNum, int pageSize, String username, Integer groupStatus);

    void updateStatus(Long id, Integer status);

    void changePassword(Long id, String oldPassword, String newPassword);

    /** 管理员创建用户，支持指定角色和上级组长 */
    User createUser(UserCreateRequest request);

    void deleteUser(Long id);

    /** 管理员给组长增加可分配额度（delta 可为负数，表示减少） */
    void assignQuotaToLeader(Long leaderId, Long delta);

    /** 原子扣减组长可分配额度，返回扣减后剩余值；余额不足时抛出异常 */
    void deductLeaderQuota(Long leaderId, Long amount);

    /** 归还额度到组长（吊销/减少成员Key时调用） */
    void returnLeaderQuota(Long leaderId, Long amount);

    /** 管理员设置用户余额（直接设为指定值） */
    void adminUpdateMoneyQuota(Long id, BigDecimal moneyQuota);

    /** 管理员充值/扣减用户余额（delta>0 增加，<0 减少） */
    void rechargeUserMoney(Long id, BigDecimal delta);

    /** 充值/扣减用户余额，指定日志类型和备注 */
    void rechargeUserMoney(Long id, BigDecimal delta, String logType, String remark);

    /** 原子累加用户已用金额统计（仅统计，不影响余额） */
    void addUsedMoney(Long id, BigDecimal amount);

    /** 原子扣减用户余额，返回是否扣减成功（余额不足或不限制时返回 false） */
    boolean deductMoneyQuota(Long id, BigDecimal amount);

    /** 管理员修改用户角色 */
    void updateRoleId(Long id, Long roleId);

    /** 重置用户密码为 123456 */
    void resetPassword(Long id);

    /** 批量设置用户金额额度 */
    void batchUpdateMoneyQuota(List<Long> userIds, BigDecimal moneyQuota);

    /** 管理员给组长充值可分配金额池（delta 可为负数减少） */
    void assignMoneyToLeader(Long leaderId, BigDecimal delta);

    /** 原子增减用户可分配金额池 */
    void addAllocatableMoney(Long id, BigDecimal delta);

    /** 扣减组长可分配金额池，余额不足时抛出异常 */
    void deductAllocatableMoney(Long leaderId, BigDecimal amount);

    /** 归还金额到组长可分配池 */
    void returnAllocatableMoney(Long leaderId, BigDecimal amount);

    // ── 组归属相关（供 GroupService / TeamService 调用）──

    /** 设置用户组状态（0=组外, 1=组内） */
    void setGroupStatus(Long userId, Integer groupStatus);

    /** 设置用户的组归属（同时设置 group_id 和 parent_id） */
    void setUserGroup(Long userId, Long groupId, Long parentId);

    /** 仅设置用户的 group_id（不改 parent_id，用于组长自身） */
    void setUserGroupId(Long userId, Long groupId);

    /** 清空用户的组归属（group_id 和 parent_id 均置 null） */
    void clearUserGroup(Long userId);

    /** 清空某组所有成员的组归属（批量置 null） */
    void clearGroupMembers(Long groupId);

    /** 将某组内所有成员（排除 excludeUserId）的 parent_id 改为 newParentId */
    void updateMembersParentId(Long groupId, Long excludeUserId, Long newParentId);

    // ── 名称填充 ──

    /** 批量填充用户的 groupName、parentName、roleName（瞬态字段） */
    void enrichUsers(List<User> users);

    /** 查找重复账号（学号或邮箱相同的用户分组）；keyword 非空时按关键词过滤 */
    List<com.aitoken.dto.response.DuplicateGroupVO> findDuplicateUsers(String keyword);

    // ── 冻结账号 ──

    /** 解冻账号（将 deleted 置回 0） */
    void restoreUser(Long id);

    /** 分页查询已冻结账号 */
    Page<User> pageDeletedUsers(int pageNum, int pageSize, String username);

    /** 填充单个用户的 groupName、parentName、roleName */
    default void enrichUser(User user) {
        if (user != null) enrichUsers(Collections.singletonList(user));
    }
}
