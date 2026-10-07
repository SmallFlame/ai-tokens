package com.aitoken.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.aitoken.common.exception.BizException;
import com.aitoken.dto.response.GroupVO;
import com.aitoken.entity.Group;
import com.aitoken.entity.GroupRechargeLog;
import com.aitoken.entity.User;
import com.aitoken.mapper.GroupMapper;
import com.aitoken.mapper.GroupRechargeLogMapper;
import com.aitoken.service.GroupService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    @Resource
    private UserService userService;

    @Resource
    private GroupRechargeLogMapper groupRechargeLogMapper;

    @Override
    public List<GroupVO> listGroups(String name) {
        return baseMapper.listGroupsWithDetail(name == null || name.isEmpty() ? null : name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Group createGroup(String name, String remark, Long leaderId, Integer reportType) {
        long exists = count(new LambdaQueryWrapper<Group>().eq(Group::getLeaderId, leaderId));
        if (exists > 0) {
            throw BizException.of("该用户已是某个小组的组长，不能重复创建");
        }
        Group group = new Group();
        group.setName(name);
        group.setRemark(remark);
        group.setLeaderId(leaderId);
        group.setReportType(reportType == null ? 1 : reportType);
        save(group);

        // 将组长自己的 group_id 设为该组
        userService.setUserGroupId(leaderId, group.getId());
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long id, String name, String remark, Long leaderId, Integer reportType) {
        Group group = getById(id);
        if (group == null) throw BizException.of("小组不存在");

        Long oldLeaderId = group.getLeaderId();
        if (leaderId != null && !leaderId.equals(oldLeaderId)) {
            long conflict = count(new LambdaQueryWrapper<Group>()
                    .eq(Group::getLeaderId, leaderId)
                    .ne(Group::getId, id));
            if (conflict > 0) throw BizException.of("该用户已是另一个小组的组长");

            // 清除旧组长的 group_id（若其 group_id 指向本组）
            User oldLeader = userService.getById(oldLeaderId);
            if (oldLeader != null && id.equals(oldLeader.getGroupId())) {
                userService.clearUserGroup(oldLeaderId);
            }

            // 将所有组员的 parent_id 改为新组长
            userService.updateMembersParentId(id, leaderId, leaderId);

            // 设置新组长的 group_id
            userService.setUserGroupId(leaderId, id);

            group.setLeaderId(leaderId);
        }

        if (name != null) group.setName(name);
        if (remark != null) group.setRemark(remark);
        if (reportType != null) group.setReportType(reportType);
        updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        Group group = getById(id);
        if (group == null) throw BizException.of("小组不存在");

        // 归还所有组员（非组长）的余额给小组金额池，并清零其余额
        if (group.getLeaderId() != null) {
            List<User> members = userService.list(new LambdaQueryWrapper<User>()
                    .eq(User::getGroupId, id)
                    .ne(User::getId, group.getLeaderId()));
            List<Long> memberIds = new java.util.ArrayList<>();
            BigDecimal totalReturn = BigDecimal.ZERO;
            for (User m : members) {
                memberIds.add(m.getId());
                BigDecimal mq = m.getMoneyQuota();
                if (mq != null && mq.compareTo(BigDecimal.ZERO) > 0) {
                    totalReturn = totalReturn.add(mq);
                }
            }
            if (totalReturn.compareTo(BigDecimal.ZERO) > 0) {
                returnMoneyPool(id, totalReturn);
            }
            if (!memberIds.isEmpty()) {
                userService.batchUpdateMoneyQuota(memberIds, BigDecimal.ZERO);
            }
        }

        userService.clearGroupMembers(id);
        removeById(id);
    }

    @Override
    public List<User> listGroupMembers(Long groupId) {
        List<User> members = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getGroupId, groupId)
                .orderByAsc(User::getId));
        userService.enrichUsers(members);
        return members;
    }

    @Override
    public List<User> listReportStudents() {
        // 所有需交周报的小组
        List<Group> reportGroups = list(new LambdaQueryWrapper<Group>()
                .eq(Group::getReportType, 1));
        if (reportGroups.isEmpty()) return new java.util.ArrayList<>();

        List<Long> groupIds = reportGroups.stream()
                .map(Group::getId)
                .collect(java.util.stream.Collectors.toList());

        // 这些组内、且状态为组内的学生
        List<User> students = userService.list(new LambdaQueryWrapper<User>()
                .in(User::getGroupId, groupIds)
                .eq(User::getGroupStatus, 1)
                .orderByAsc(User::getGroupId)
                .orderByAsc(User::getId));
        userService.enrichUsers(students);
        return students;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long groupId, Long userId) {
        Group group = getById(groupId);
        if (group == null) throw BizException.of("小组不存在");

        User user = userService.getById(userId);
        if (user == null) throw BizException.of("用户不存在");

        // 如果用户已在其他组，迁移到新组（余额跟人走）
        if (user.getGroupId() != null) {
            userService.clearUserGroup(userId);
        }

        userService.setUserGroup(userId, groupId, group.getLeaderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, Long userId) {
        User user = userService.getById(userId);
        if (user == null) throw BizException.of("用户不存在");
        if (!groupId.equals(user.getGroupId())) throw BizException.of("该用户不在此小组中");

        // 归还剩余余额给小组金额池
        BigDecimal mq = user.getMoneyQuota();
        if (mq != null && mq.compareTo(BigDecimal.ZERO) > 0) {
            returnMoneyPool(groupId, mq);
        }
        userService.adminUpdateMoneyQuota(userId, BigDecimal.ZERO);

        userService.clearUserGroup(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductMoneyPool(Long groupId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Group group = getById(groupId);
        BigDecimal before = group != null && group.getMoneyPool() != null ? group.getMoneyPool() : BigDecimal.ZERO;
        int rows = baseMapper.addMoneyPool(groupId, amount.negate());
        if (rows == 0) throw BizException.of("小组金额池余额不足");
        saveGroupRechargeLog(groupId, "allocate", amount.negate(), before, before.subtract(amount), "分给组员");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnMoneyPool(Long groupId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Group group = getById(groupId);
        BigDecimal before = group != null && group.getMoneyPool() != null ? group.getMoneyPool() : BigDecimal.ZERO;
        baseMapper.addMoneyPool(groupId, amount);
        saveGroupRechargeLog(groupId, "return", amount, before, before.add(amount), "组员余额归还");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargeMoneyPool(Long groupId, BigDecimal delta) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) return;
        Group group = getById(groupId);
        BigDecimal before = group != null && group.getMoneyPool() != null ? group.getMoneyPool() : BigDecimal.ZERO;
        int rows = baseMapper.addMoneyPool(groupId, delta);
        if (rows == 0 && delta.compareTo(BigDecimal.ZERO) < 0) {
            throw BizException.of("小组金额池余额不足");
        }
        saveGroupRechargeLog(groupId, "recharge", delta, before, before.add(delta),
                delta.compareTo(BigDecimal.ZERO) > 0 ? "管理员充值" : "管理员扣减");
    }

    // ── 小组充值日志 ──

    private void saveGroupRechargeLog(Long groupId, String type, BigDecimal amount,
                                       BigDecimal before, BigDecimal after, String remark) {
        Group group = getById(groupId);
        GroupRechargeLog log = new GroupRechargeLog();
        log.setGroupId(groupId);
        log.setGroupName(group != null ? group.getName() : "");
        log.setOperatorId(getGroupOpId());
        log.setOperatorName(getGroupOpName());
        log.setType(type);
        log.setAmount(amount);
        log.setBeforeBalance(before);
        log.setAfterBalance(after);
        log.setRemark(remark);
        groupRechargeLogMapper.insert(log);
    }

    private Long getGroupOpId() {
        try { return StpUtil.getLoginIdAsLong(); }
        catch (Exception e) { return 0L; }
    }

    private String getGroupOpName() {
        try {
            Long id = StpUtil.getLoginIdAsLong();
            User op = userService.getById(id);
            return op != null ? op.getUsername() : "系统";
        } catch (Exception e) { return "系统"; }
    }
}
