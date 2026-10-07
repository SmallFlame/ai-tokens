package com.aitoken.service;

import com.aitoken.dto.response.GroupVO;
import com.aitoken.entity.Group;
import com.aitoken.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface GroupService extends IService<Group> {

    List<GroupVO> listGroups(String name);

    Group createGroup(String name, String remark, Long leaderId, Integer reportType);

    void updateGroup(Long id, String name, String remark, Long leaderId, Integer reportType);

    void deleteGroup(Long id);

    List<User> listGroupMembers(Long groupId);

    /** 查询所有「需交周报」组内的学生 */
    List<User> listReportStudents();

    /** 管理员将已有用户拉入小组 */
    void addMember(Long groupId, Long userId);

    /** 管理员将用户从小组移出（不删除账号） */
    void removeMember(Long groupId, Long userId);

    /** 从小组金额池扣减（组长分配给成员时） */
    void deductMoneyPool(Long groupId, BigDecimal amount);

    /** 归还到小组金额池（成员移出或减少额度时） */
    void returnMoneyPool(Long groupId, BigDecimal amount);

    /** 管理员充值小组金额池 */
    void rechargeMoneyPool(Long groupId, BigDecimal delta);
}
