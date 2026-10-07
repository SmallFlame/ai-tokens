package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.dto.response.GroupVO;
import com.aitoken.entity.Group;
import com.aitoken.entity.User;
import com.aitoken.service.GroupService;
import com.aitoken.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "小组管理（管理员）")
@RestController
@RequestMapping("/api/admin/groups")
public class AdminGroupController {

    @Resource
    private GroupService groupService;

    @Resource
    private UserService userService;

    @Operation(summary = "查询小组列表")
    @GetMapping
    public Result<List<GroupVO>> list(@RequestParam(required = false) String name) {
        return Result.ok(groupService.listGroups(name));
    }

    @Operation(summary = "创建小组")
    @PostMapping
    public Result<Group> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String remark = (String) body.get("remark");
        Long leaderId = body.get("leaderId") == null ? null : Long.valueOf(body.get("leaderId").toString());
        Integer reportType = body.get("reportType") == null ? null : Integer.valueOf(body.get("reportType").toString());
        if (name == null || name.isEmpty()) return Result.fail("小组名称不能为空");
        if (leaderId == null) return Result.fail("请指定组长");
        return Result.ok(groupService.createGroup(name, remark, leaderId, reportType));
    }

    @Operation(summary = "编辑小组")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String remark = (String) body.get("remark");
        Long leaderId = body.get("leaderId") == null ? null : Long.valueOf(body.get("leaderId").toString());
        Integer reportType = body.get("reportType") == null ? null : Integer.valueOf(body.get("reportType").toString());
        groupService.updateGroup(id, name, remark, leaderId, reportType);
        return Result.ok();
    }

    @Operation(summary = "删除小组")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return Result.ok();
    }

    @Operation(summary = "查看小组成员")
    @GetMapping("/{id}/members")
    public Result<List<User>> members(@PathVariable Long id) {
        List<User> members = groupService.listGroupMembers(id);
        members.forEach(u -> u.setPassword(null));
        return Result.ok(members);
    }

    @Operation(summary = "查询所有需交周报组的学生")
    @GetMapping("/report/members")
    public Result<List<User>> reportMembers() {
        List<User> students = groupService.listReportStudents();
        students.forEach(u -> u.setPassword(null));
        return Result.ok(students);
    }

    @Operation(summary = "将用户加入小组")
    @PostMapping("/{id}/members")
    public Result<Void> addMember(@PathVariable Long id, @RequestParam Long userId) {
        groupService.addMember(id, userId);
        return Result.ok();
    }

    @Operation(summary = "将用户移出小组（不删除账号）")
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(id, userId);
        return Result.ok();
    }

    @Operation(summary = "充值/扣减小组金额池")
    @PutMapping("/{id}/money-pool")
    public Result<Void> rechargeMoneyPool(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal delta = new BigDecimal(body.get("delta").toString());
        groupService.rechargeMoneyPool(id, delta);
        return Result.ok();
    }
}
