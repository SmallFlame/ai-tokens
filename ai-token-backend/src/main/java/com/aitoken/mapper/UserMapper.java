package com.aitoken.mapper;

import com.aitoken.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 原子增减 allocatable_quota（delta 为正增加，为负减少） */
    @Update("UPDATE t_user SET allocatable_quota = allocatable_quota + #{delta} WHERE id = #{id} AND deleted = 0")
    void updateAllocatableQuota(@Param("id") Long id, @Param("delta") Long delta);

    /** 原子累加已用金额（统计用，COALESCE 防 NULL） */
    @Update("UPDATE t_user SET used_money = COALESCE(used_money, 0) + #{amount} WHERE id = #{id} AND deleted = 0")
    void addUsedMoney(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 原子扣减余额（仅余额 >= 0 时扣减，可透支为负一次；null/-1 不扣）。返回影响行数，0 表示扣减未生效 */
    @Update("UPDATE t_user SET money_quota = money_quota - #{amount} WHERE id = #{id} AND money_quota IS NOT NULL AND money_quota >= 0 AND deleted = 0")
    int deductMoneyQuota(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 原子充值余额（money_quota += delta，delta 可为负数减少；NULL 视为 0） */
    @Update("UPDATE t_user SET money_quota = COALESCE(money_quota, 0) + #{delta} WHERE id = #{id} AND deleted = 0")
    void rechargeMoneyQuota(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /** 原子增减可分配金额池（delta 为正增加，为负减少） */
    @Update("UPDATE t_user SET allocatable_money = allocatable_money + #{delta} WHERE id = #{id} AND deleted = 0")
    void updateAllocatableMoney(@Param("id") Long id, @Param("delta") BigDecimal delta);

    // ── 冻结账号相关（需绕过 MyBatis-Plus 逻辑删除过滤，故手写 SQL）──

    /** 解冻账号 */
    @Update("UPDATE t_user SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    /** 分页查询已冻结账号（IPage 由 MyBatis-Plus 自动分页） */
    @Select("SELECT * FROM t_user WHERE deleted = 1 " +
            "AND (#{username} IS NULL OR username LIKE CONCAT('%', #{username}, '%')) " +
            "ORDER BY id DESC")
    Page<User> selectDeletedUsers(Page<User> page, @Param("username") String username);
}
