package com.aitoken.service.impl;

import com.aitoken.common.exception.BizException;
import com.aitoken.dto.request.ApiKeyCreateRequest;
import com.aitoken.dto.request.UserCreateRequest;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.Group;
import com.aitoken.entity.User;
import com.aitoken.entity.Role;
import com.aitoken.service.ApiKeyService;
import com.aitoken.service.GroupService;
import com.aitoken.service.RoleService;
import com.aitoken.service.TeamService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private GroupService groupService;

    @Override
    public List<User> listMembers(Long leaderId) {
        // 获取组长所在的小组
        List<Group> groups = groupService.list(new LambdaQueryWrapper<Group>().eq(Group::getLeaderId, leaderId));
        if (groups.isEmpty()) {
            return new ArrayList<>();
        }
        // 返回小组内所有成员（包括组长自己）
        List<User> members = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getGroupId, groups.get(0).getId())
                .orderByDesc(User::getCreatedAt));
        userService.enrichUsers(members);
        return members;
    }

    @Override
    public User createMember(Long leaderId, UserCreateRequest request) {
        request.setRoleId(roleService.getByCode("member").getId());
        request.setParentId(leaderId);
        User member = userService.createUser(request);

        // 若组长已有小组，新组员也归属该组
        List<Group> groups = groupService.list(
                new LambdaQueryWrapper<Group>().eq(Group::getLeaderId, leaderId));
        if (!groups.isEmpty()) {
            userService.setUserGroupId(member.getId(), groups.get(0).getId());
            member.setGroupId(groups.get(0).getId());
        }

        member.setPassword(null);
        return member;
    }

    @Override
    public void updateMemberStatus(Long leaderId, Long memberId, Integer status) {
        checkMemberBelongsToLeader(leaderId, memberId);
        userService.updateStatus(memberId, status);
    }

    @Override
    public void deleteMember(Long leaderId, Long memberId) {
        checkMemberBelongsToLeader(leaderId, memberId);
        userService.deleteUser(memberId);
    }

    @Override
    public List<ApiKey> listTeamKeys(Long leaderId) {
        // 组长自己的 Key
        List<ApiKey> keys = new ArrayList<>(apiKeyService.listByUser(leaderId));
        // 组员的 Key
        List<Long> memberIds = listMembers(leaderId).stream()
                .map(User::getId).collect(Collectors.toList());
        for (Long memberId : memberIds) {
            keys.addAll(apiKeyService.listByUser(memberId));
        }
        return keys;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKey createKey(Long leaderId, Long targetUserId, ApiKeyCreateRequest request) {
        // targetUserId 必须是组长自己或其组员
        if (!leaderId.equals(targetUserId)) {
            checkMemberBelongsToLeader(leaderId, targetUserId);
        }
        // 团队创建的密钥强制为金额消费型
        request.setKeyType(2);
        request.setTotalQuota(0L);
        return apiKeyService.createKey(targetUserId, request);
    }

    @Override
    public void updateKeyQuota(Long leaderId, Long keyId, Long newTotalQuota) {
        // Token额度联动已移除，此方法不再使用
        throw BizException.of(400, "Token额度型密钥管理已简化，请使用金额消费型密钥");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeKey(Long leaderId, Long keyId) {
        ApiKey key = apiKeyService.getById(keyId);
        if (key == null) throw BizException.of("Key 不存在");
        checkKeyBelongsToTeam(leaderId, key);
        apiKeyService.revokeKey(keyId);
    }

    // ── 内部校验 ──

    private void checkMemberBelongsToLeader(Long leaderId, Long memberId) {
        User member = userService.getById(memberId);
        if (member == null || !leaderId.equals(member.getParentId())) {
            throw BizException.of(403, "无权操作该用户");
        }
    }

    private void checkKeyBelongsToTeam(Long leaderId, ApiKey key) {
        Long keyOwner = key.getUserId();
        if (leaderId.equals(keyOwner)) return; // 组长自己的Key
        // 否则必须是组长的某个组员的Key
        User owner = userService.getById(keyOwner);
        if (owner == null || !leaderId.equals(owner.getParentId())) {
            throw BizException.of(403, "无权操作该 Key");
        }
    }

    @Override
    public List<User> getAvailableUsers() {
        Role memberRole = roleService.getByCode("member");
        List<User> users = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getRoleId, memberRole.getId())
                .orderByDesc(User::getCreatedAt));
        userService.enrichUsers(users);
        return users;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pullMember(Long leaderId, Long userId) {
        List<Group> groups = groupService.list(
                new LambdaQueryWrapper<Group>().eq(Group::getLeaderId, leaderId));
        if (groups.isEmpty()) throw BizException.of("你还没有小组，请联系管理员创建");

        Group group = groups.get(0);
        User user = userService.getById(userId);
        if (user == null) throw BizException.of("用户不存在");

        // 如果用户已在其他组，迁移到新组（余额跟人走）
        if (user.getGroupId() != null) {
            userService.clearUserGroup(userId);
        }

        userService.setUserGroup(userId, group.getId(), leaderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseMember(Long leaderId, Long userId) {
        User member = userService.getById(userId);
        if (member == null) throw BizException.of("用户不存在");
        if (!leaderId.equals(member.getParentId())) {
            throw BizException.of(403, "无权操作该用户");
        }

        // 归还剩余余额给小组金额池
        BigDecimal mq = member.getMoneyQuota();
        if (mq != null && mq.compareTo(BigDecimal.ZERO) > 0) {
            Long groupId = member.getGroupId();
            if (groupId != null) {
                groupService.returnMoneyPool(groupId, mq);
            }
        }
        userService.adminUpdateMoneyQuota(userId, BigDecimal.ZERO);

        userService.clearUserGroup(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustMemberMoney(Long leaderId, Long memberId, BigDecimal delta) {
        // 允许组长给自己分配，或给组员分配
        if (!leaderId.equals(memberId)) {
            checkMemberBelongsToLeader(leaderId, memberId);
        }
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) return;

        User member = userService.getById(memberId);
        Long groupId = member.getGroupId();
        if (groupId == null) throw BizException.of("该用户不在任何小组中");

        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            // 新增：从小组金额池扣减，组员余额增加
            groupService.deductMoneyPool(groupId, delta);
            userService.rechargeUserMoney(memberId, delta, "member_adjust", "组长分配");
        } else {
            // 减少：检查组员余额是否充足后扣减，归还给小组金额池
            BigDecimal currentBalance = member.getMoneyQuota();
            if (currentBalance == null || currentBalance.compareTo(BigDecimal.ZERO) < 0) {
                currentBalance = BigDecimal.ZERO;
            }
            BigDecimal abs = delta.negate();
            if (currentBalance.compareTo(abs) < 0) {
                throw BizException.of("组员余额（¥" + currentBalance.toPlainString() + "）不足，无法减少 ¥" + abs.toPlainString());
            }
            userService.rechargeUserMoney(memberId, delta, "member_adjust", "组长回收");
            groupService.returnMoneyPool(groupId, abs);
        }
    }
}
