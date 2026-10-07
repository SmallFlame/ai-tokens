package com.aitoken.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.aitoken.common.exception.BizException;
import com.aitoken.dto.request.LoginRequest;
import com.aitoken.dto.request.RegisterRequest;
import com.aitoken.dto.request.UserCreateRequest;
import com.aitoken.dto.response.LoginResponse;
import com.aitoken.entity.Group;
import com.aitoken.entity.Role;
import com.aitoken.entity.User;
import com.aitoken.entity.UserRechargeLog;
import com.aitoken.mapper.UserMapper;
import com.aitoken.mapper.UserRechargeLogMapper;
import com.aitoken.service.GroupService;
import com.aitoken.service.RoleService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Lazy
    @Resource
    private GroupService groupService;

    @Resource
    private RoleService roleService;

    @Resource
    private UserRechargeLogMapper userRechargeLogMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw BizException.of(401, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw BizException.of(403, "账号已被禁用");
        }
        StpUtil.login(user.getId());

        // 从 t_role 表获取角色信息
        Role role = null;
        Long roleId = user.getRoleId();
        if (roleId == null) {
            // 兼容旧数据：从 role 字段推断
            roleId = roleIdFromLegacyRole(user.getRole());
        }
        if (roleId != null) {
            role = roleService.getById(roleId);
        }
        String roleCode = role != null ? role.getCode() : "member";
        String roleName = role != null ? role.getName() : "组员";

        StpUtil.getSession().set("roleCode", roleCode);
        StpUtil.getSession().set("roleName", roleName);

        // 查询小组信息
        String groupName = null;
        if (user.getGroupId() != null) {
            Group group = groupService.getById(user.getGroupId());
            if (group != null) {
                groupName = group.getName();
            }
        }

        LoginResponse resp = new LoginResponse();
        resp.setToken(StpUtil.getTokenValue());
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setRoleCode(roleCode);
        resp.setRoleName(roleName);
        resp.setGroupId(user.getGroupId());
        resp.setGroupName(groupName);
        resp.setStudentId(user.getStudentId());
        resp.setAvatar(user.getAvatar());
        resp.setEmail(user.getEmail());
        return resp;
    }

    /** 兼容旧数据：将旧 role 整数映射为 role_id */
    private Long roleIdFromLegacyRole(Integer role) {
        if (role == null) return 4L; // 默认组员
        switch (role) {
            case 0: return 1L; // super_admin
            case 3: return 2L; // advisor
            case 1: return 3L; // leader
            case 2:
            default: return 4L; // member
        }
    }

    @Override
    public User register(RegisterRequest request) {
        long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw BizException.of("用户名已存在");
        }
        // 获取组员角色ID
        Role memberRole = roleService.getByCode("member");
        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setStudentId(request.getStudentId());
        user.setRoleId(memberRole.getId()); // 自注册默认为组员
        user.setStatus(1);
        user.setGroupStatus(0); // 默认组外
        user.setAllocatableQuota(0L);
        user.setMoneyQuota(BigDecimal.ZERO);
        save(user);
        enrichUser(user);
        return user;
    }

    @Override
    public Page<User> pageUsers(int pageNum, int pageSize, String username, Integer status, Integer groupStatus) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(username != null && !username.isEmpty(), User::getUsername, username)
                .eq(status != null, User::getStatus, status)
                .eq(groupStatus != null, User::getGroupStatus, groupStatus)
                .orderByDesc(User::getCreatedAt);
        Page<User> result = page(new Page<>(pageNum, pageSize), wrapper);
        enrichUsers(result.getRecords());
        return result;
    }

    @Override
    public Page<User> pageOutGroupUsers(int pageNum, int pageSize, String username, Integer groupStatus) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(groupStatus != null, User::getGroupStatus, groupStatus)
                .like(username != null && !username.isEmpty(), User::getUsername, username)
                .orderByDesc(User::getCreatedAt);
        Page<User> result = page(new Page<>(pageNum, pageSize), wrapper);
        enrichUsers(result.getRecords());
        return result;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = getById(id);
        if (user == null) throw BizException.of("用户不存在");
        user.setStatus(status);
        updateById(user);
    }

    @Override
    public void resetPassword(Long id) {
        User user = getById(id);
        if (user == null) throw BizException.of("用户不存在");
        user.setPassword(BCrypt.hashpw("123456"));
        updateById(user);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = getById(id);
        if (user == null) throw BizException.of("用户不存在");
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw BizException.of("原密码错误");
        }
        user.setPassword(BCrypt.hashpw(newPassword));
        updateById(user);
    }

    @Override
    public User createUser(UserCreateRequest request) {
        long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw BizException.of("用户名已存在");
        }
        Long roleId = request.getRoleId() == null ? 4L : request.getRoleId();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setStudentId(request.getStudentId());
        user.setRoleId(roleId);
        user.setStatus(1);
//        可分配额度与当前额度都是0
        user.setAllocatableQuota(0L);
        user.setMoneyQuota(BigDecimal.ZERO);
        save(user);
        enrichUser(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) throw BizException.of("用户不存在");
        Role role = roleService.getById(user.getRoleId());
        if (role != null && "super_admin".equals(role.getCode())) throw BizException.of("不能删除管理员");
        // 若用户在组内且有余额，归还给小组金额池
        BigDecimal mq = user.getMoneyQuota();
        if (mq != null && mq.compareTo(BigDecimal.ZERO) > 0 && user.getGroupId() != null) {
            groupService.returnMoneyPool(user.getGroupId(), mq);
        }
        // 清理组归属，避免冻结用户仍占用小组成员席位
        clearUserGroup(id);
        // 逻辑删除（@TableLogic 自动转为 UPDATE deleted = 1）
        removeById(id);
    }

    @Override
    public void restoreUser(Long id) {
        int rows = baseMapper.restoreById(id);
        if (rows == 0) throw BizException.of("账号不存在或未被冻结");
    }

    @Override
    public Page<User> pageDeletedUsers(int pageNum, int pageSize, String username) {
        return baseMapper.selectDeletedUsers(
                new Page<>(pageNum, pageSize),
                username == null || username.isEmpty() ? null : username);
    }
    // 管理员给组长增加可分配额度
    @Override
    public void assignQuotaToLeader(Long leaderId, Long delta) {
        User leader = getById(leaderId);
        if (leader == null) throw BizException.of("用户不存在");
        Role role = roleService.getById(leader.getRoleId());
        if (role == null || (!"super_admin".equals(role.getCode()) && !"leader".equals(role.getCode()))) {
            throw BizException.of("只能给组长分配额度");
        }
        baseMapper.updateAllocatableQuota(leaderId, delta);
    }
    /** 原子扣减组长可分配额度，返回扣减后剩余值；余额不足时抛出异常 */
    @Override
    public void deductLeaderQuota(Long leaderId, Long amount) {
        User leader = getById(leaderId);
        if (leader == null) throw BizException.of("组长不存在");
        long current = leader.getAllocatableQuota() == null ? 0L : leader.getAllocatableQuota();
        if (current < amount) {
            throw BizException.of("可分配额度不足，当前剩余: " + current);
        }
        baseMapper.updateAllocatableQuota(leaderId, -amount);
    }

    @Override
    public void returnLeaderQuota(Long leaderId, Long amount) {
        if (amount <= 0) return;
        baseMapper.updateAllocatableQuota(leaderId, amount);
    }

    /** 管理员设置用户余额（直接设为指定值） */
    @Override
    public void adminUpdateMoneyQuota(Long id, BigDecimal moneyQuota) {
        User user = getById(id);
        if (user == null) throw BizException.of("用户不存在");
        BigDecimal before = user.getMoneyQuota() != null ? user.getMoneyQuota() : BigDecimal.ZERO;
        lambdaUpdate().eq(User::getId, id).set(User::getMoneyQuota, moneyQuota).update();
        BigDecimal after = moneyQuota != null ? moneyQuota : BigDecimal.ZERO;
        saveRechargeLog(id, user.getUsername(), getOperatorId(), getOperatorName(),
                "set_balance", after.subtract(before), before, after, "管理员设置余额");
    }

    /** 管理员充值/扣减用户余额（delta>0 增加，<0 减少） */
    @Override
    public void rechargeUserMoney(Long id, BigDecimal delta) {
        rechargeUserMoney(id, delta, "recharge", null);
    }

    @Override
    public void rechargeUserMoney(Long id, BigDecimal delta, String logType, String remark) {
        if (delta == null) throw BizException.of("金额不能为空");
        User user = getById(id);
        BigDecimal before = user != null && user.getMoneyQuota() != null ? user.getMoneyQuota() : BigDecimal.ZERO;
        baseMapper.rechargeMoneyQuota(id, delta);
        user = getById(id);
        BigDecimal after = user != null && user.getMoneyQuota() != null ? user.getMoneyQuota() : before.add(delta);
        saveRechargeLog(id, user != null ? user.getUsername() : "",
                getOperatorId(), getOperatorName(),
                logType != null ? logType : "recharge", delta, before, after, remark);
    }

    /** 原子累加用户已用金额统计（仅统计，不影响余额） */
    @Override
    public void addUsedMoney(Long id, BigDecimal amount) {
        baseMapper.addUsedMoney(id, amount);
    }

    /** 原子扣减用户余额，返回是否扣减成功 */
    @Override
    public boolean deductMoneyQuota(Long id, BigDecimal amount) {
        return baseMapper.deductMoneyQuota(id, amount) > 0;
    }

    /** 管理员修改用户角色 */
    @Override
    public void updateRoleId(Long id, Long roleId) {
        User user = getById(id);
        if (user == null) throw BizException.of("用户不存在");
        // 通过 roleId 查角色，检查是否试图修改超管
        Role targetRole = roleService.getById(roleId);
        if (targetRole == null) throw BizException.of("角色不存在");
        if ("super_admin".equals(targetRole.getCode()) && user.getRoleId() != null
                && !user.getRoleId().equals(1L)) {
            throw BizException.of("不能将普通用户设为超级管理员");
        }
        lambdaUpdate().eq(User::getId, id).set(User::getRoleId, roleId).update();
    }

    @Override
    public void batchUpdateMoneyQuota(List<Long> userIds, BigDecimal moneyQuota) {
        if (userIds == null || userIds.isEmpty()) return;
        Long operatorId = getOperatorId();
        String operatorName = getOperatorName();
        BigDecimal target = moneyQuota != null ? moneyQuota : BigDecimal.ZERO;
        for (Long uid : userIds) {
            User u = getById(uid);
            BigDecimal before = u != null && u.getMoneyQuota() != null ? u.getMoneyQuota() : BigDecimal.ZERO;
            lambdaUpdate().eq(User::getId, uid).set(User::getMoneyQuota, target).update();
            saveRechargeLog(uid, u != null ? u.getUsername() : "",
                    operatorId, operatorName,
                    "batch_set", target.subtract(before), before, target, "批量设置余额");
        }
    }

    @Override
    public void assignMoneyToLeader(Long leaderId, BigDecimal delta) {
        User leader = getById(leaderId);
        if (leader == null) throw BizException.of("用户不存在");
        Role role = roleService.getById(leader.getRoleId());
        if (role == null || (!"super_admin".equals(role.getCode()) && !"leader".equals(role.getCode()))) {
            throw BizException.of("只能给组长分配金额池");
        }
        BigDecimal before = leader.getAllocatableMoney() != null ? leader.getAllocatableMoney() : BigDecimal.ZERO;
        baseMapper.updateAllocatableMoney(leaderId, delta);
        saveRechargeLog(leaderId, leader.getUsername(), getOperatorId(), getOperatorName(),
                "assign_pool", delta, before, before.add(delta), "组长可分配金额池变动");
    }

    @Override
    public void addAllocatableMoney(Long id, BigDecimal delta) {
        baseMapper.updateAllocatableMoney(id, delta);
    }

    @Override
    public void deductAllocatableMoney(Long leaderId, BigDecimal amount) {
        User leader = getById(leaderId);
        if (leader == null) throw BizException.of("组长不存在");
        BigDecimal current = leader.getAllocatableMoney() == null ? BigDecimal.ZERO : leader.getAllocatableMoney();
        if (current.compareTo(amount) < 0) {
            throw BizException.of("可分配金额不足，当前剩余: ¥" + current.toPlainString());
        }
        baseMapper.updateAllocatableMoney(leaderId, amount.negate());
    }

    @Override
    public void returnAllocatableMoney(Long leaderId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        baseMapper.updateAllocatableMoney(leaderId, amount);
    }

    @Override
    public void setUserGroup(Long userId, Long groupId, Long parentId) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getGroupId, groupId)
                .set(User::getParentId, parentId)
                .set(User::getGroupStatus, 1)
                .update();
    }

    @Override
    public void setUserGroupId(Long userId, Long groupId) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getGroupId, groupId)
                .set(User::getGroupStatus, 1)
                .update();
    }

    @Override
    public void clearUserGroup(Long userId) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getGroupId, null)
                .set(User::getParentId, null)
                .set(User::getGroupStatus, 0)
                .update();
    }

    @Override
    public void clearGroupMembers(Long groupId) {
        lambdaUpdate()
                .eq(User::getGroupId, groupId)
                .set(User::getGroupId, null)
                .set(User::getParentId, null)
                .set(User::getGroupStatus, 0)
                .update();
    }

    @Override
    public void updateMembersParentId(Long groupId, Long excludeUserId, Long newParentId) {
        lambdaUpdate()
                .eq(User::getGroupId, groupId)
                .ne(User::getId, excludeUserId)
                .set(User::getParentId, newParentId)
                .update();
    }

    @Override
    public void setGroupStatus(Long userId, Integer groupStatus) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getGroupStatus, groupStatus)
                .update();
    }

    // ── 充值日志写入 ──

    @Override
    public void enrichUsers(List<User> users) {
        if (users == null || users.isEmpty()) return;

        // 1. 收集各类 ID
        Set<Long> groupIds = users.stream()
                .map(User::getGroupId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> parentIds = users.stream()
                .map(User::getParentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> roleIds = users.stream()
                .map(User::getRoleId).filter(Objects::nonNull).collect(Collectors.toSet());

        // 2. 批量查询映射
        Map<Long, String> groupNameMap = new HashMap<>();
        if (!groupIds.isEmpty()) {
            groupService.listByIds(groupIds)
                    .forEach(g -> groupNameMap.put(g.getId(), g.getName()));
        }

        Map<Long, String> parentNameMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            listByIds(parentIds).forEach(p ->
                    parentNameMap.put(p.getId(),
                            p.getNickname() != null && !p.getNickname().isEmpty()
                                    ? p.getNickname() : p.getUsername()));
        }

        Map<Long, String> roleNameMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            roleService.listByIds(roleIds)
                    .forEach(r -> roleNameMap.put(r.getId(), r.getName()));
        }

        // 3. 赋值
        for (User u : users) {
            u.setGroupName(groupNameMap.get(u.getGroupId()));
            u.setParentName(parentNameMap.get(u.getParentId()));
            u.setRoleName(roleNameMap.get(u.getRoleId()));
        }
    }

    private Long getOperatorId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String getOperatorName() {
        try {
            Long id = StpUtil.getLoginIdAsLong();
            User op = getById(id);
            return op != null ? op.getUsername() : "系统";
        } catch (Exception e) {
            return "系统";
        }
    }

    /** 写入用户充值日志（供本类及外部调用） */
    public void saveRechargeLog(Long userId, String username, Long operatorId, String operatorName,
                                String type, BigDecimal amount, BigDecimal beforeBalance,
                                BigDecimal afterBalance, String remark) {
        UserRechargeLog log = new UserRechargeLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setType(type);
        log.setAmount(amount);
        log.setBeforeBalance(beforeBalance);
        log.setAfterBalance(afterBalance);
        log.setRemark(remark);
        userRechargeLogMapper.insert(log);
    }

    // ── 重复账号排查 ──

    @Override
    public List<com.aitoken.dto.response.DuplicateGroupVO> findDuplicateUsers(String keyword) {
        List<User> all = list();
        if (all.isEmpty()) return new ArrayList<>();

        // 并查集：把学号或邮箱相同的用户合并到一组（支持链式关联）
        Map<Long, Long> parent = new HashMap<>();
        for (User u : all) parent.put(u.getId(), u.getId());

        unionByField(all, User::getStudentId, parent);
        unionByField(all, User::getEmail, parent);

        // 按 root 聚合
        Map<Long, List<User>> groups = new LinkedHashMap<>();
        for (User u : all) {
            groups.computeIfAbsent(find(parent, u.getId()), k -> new ArrayList<>()).add(u);
        }

        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        List<com.aitoken.dto.response.DuplicateGroupVO> result = new ArrayList<>();
        for (List<User> members : groups.values()) {
            if (members.size() < 2) continue;

            // 关键词过滤：组内任一用户命中即保留
            if (kw != null && !kw.isEmpty() && members.stream().noneMatch(u -> matchKeyword(u, kw))) {
                continue;
            }

            com.aitoken.dto.response.DuplicateGroupVO vo = new com.aitoken.dto.response.DuplicateGroupVO();

            String commonStudentId = commonValue(members, User::getStudentId);
            String commonEmail     = commonValue(members, User::getEmail);
            List<String> matched = new ArrayList<>();
            if (commonStudentId != null) matched.add("studentId");
            if (commonEmail != null)     matched.add("email");

            vo.setMatchedFields(matched);
            vo.setStudentId(commonStudentId);
            vo.setEmail(commonEmail);

            enrichUsers(members);
            members.forEach(u -> u.setPassword(null));
            vo.setUsers(members);
            result.add(vo);
        }

        // 组内人数多的排前面
        result.sort((a, b) -> Integer.compare(b.getUsers().size(), a.getUsers().size()));
        return result;
    }

    /** 按某个字段的值把用户 union 到一起（空值跳过） */
    private void unionByField(List<User> users, java.util.function.Function<User, String> getter,
                              Map<Long, Long> parent) {
        Map<String, Long> firstByValue = new HashMap<>();
        for (User u : users) {
            String value = getter.apply(u);
            if (value == null || value.trim().isEmpty()) continue;
            String key = value.trim().toLowerCase();
            Long firstId = firstByValue.get(key);
            if (firstId == null) {
                firstByValue.put(key, u.getId());
            } else {
                union(parent, firstId, u.getId());
            }
        }
    }

    private Long find(Map<Long, Long> parent, Long id) {
        Long root = id;
        while (!root.equals(parent.get(root))) {
            root = parent.get(root);
        }
        // 路径压缩
        Long cur = id;
        while (!cur.equals(root)) {
            Long next = parent.get(cur);
            parent.put(cur, root);
            cur = next;
        }
        return root;
    }

    private void union(Map<Long, Long> parent, Long a, Long b) {
        Long ra = find(parent, a);
        Long rb = find(parent, b);
        if (!ra.equals(rb)) parent.put(rb, ra);
    }

    /** 返回组内该字段的共同值（全部相同且非空），否则返回 null */
    private String commonValue(List<User> users, java.util.function.Function<User, String> getter) {
        String first = null;
        for (User u : users) {
            String v = getter.apply(u);
            if (v == null || v.trim().isEmpty()) return null;
            if (first == null) first = v.trim();
            else if (!first.equals(v.trim())) return null;
        }
        return first;
    }

    private boolean matchKeyword(User u, String kw) {
        return contains(u.getUsername(), kw)
                || contains(u.getNickname(), kw)
                || contains(u.getStudentId(), kw)
                || contains(u.getEmail(), kw);
    }

    private boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }
}
