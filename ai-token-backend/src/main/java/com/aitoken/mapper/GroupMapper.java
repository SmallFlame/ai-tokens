package com.aitoken.mapper;

import com.aitoken.dto.response.GroupVO;
import com.aitoken.entity.Group;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface GroupMapper extends BaseMapper<Group> {

    @Select("SELECT g.id, g.name, g.remark, g.leader_id, g.created_at, g.money_pool, g.report_type, " +
            "u.username AS leader_name, " +
            "(SELECT COUNT(*) FROM t_user WHERE group_id = g.id AND id != g.leader_id AND deleted = 0) AS member_count, " +
            "(SELECT COALESCE(SUM(CASE WHEN money_quota IS NULL THEN 0 ELSE money_quota END), 0) " +
            " FROM t_user WHERE group_id = g.id AND deleted = 0) AS total_money " +
            "FROM t_group g LEFT JOIN t_user u ON g.leader_id = u.id AND u.deleted = 0 " +
            "WHERE (#{name} IS NULL OR g.name LIKE CONCAT('%',#{name},'%')) " +
            "ORDER BY g.created_at DESC")
    List<GroupVO> listGroupsWithDetail(@Param("name") String name);

    @Update("UPDATE t_group SET money_pool = money_pool + #{amount} WHERE id = #{id} AND money_pool + #{amount} >= 0")
    int addMoneyPool(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
