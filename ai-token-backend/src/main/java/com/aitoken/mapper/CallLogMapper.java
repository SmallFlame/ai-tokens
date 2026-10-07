package com.aitoken.mapper;

import com.aitoken.dto.response.ChannelStatVO;
import com.aitoken.dto.response.TrendVO;
import com.aitoken.dto.response.UserStatVO;
import com.aitoken.entity.CallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface CallLogMapper extends BaseMapper<CallLog> {

    List<TrendVO> selectDailyTrend(@Param("days") int days);

    /** 仅统计组外用户（group_status=0）的每日趋势 */
    List<TrendVO> selectDailyTrendByOutGroup(@Param("days") int days);

    int deleteBeforeDate(@Param("date") Date date);

    @Select("SELECT IFNULL(COUNT(*),0) FROM t_call_log WHERE user_id=#{userId} AND status=1 AND DATE(created_at)=CURDATE()")
    long countTodayByUser(@Param("userId") Long userId);

    @Select("SELECT IFNULL(SUM(total_tokens),0) FROM t_call_log WHERE user_id=#{userId} AND status=1 AND DATE(created_at)=CURDATE()")
    long sumTodayTokensByUser(@Param("userId") Long userId);

    @Select("SELECT IFNULL(SUM(cost),0) FROM t_call_log WHERE user_id=#{userId} AND status=1 AND DATE(created_at)=CURDATE()")
    BigDecimal sumTodayCostByUser(@Param("userId") Long userId);

    @Select("SELECT IFNULL(COUNT(*),0) FROM t_call_log WHERE user_id=#{userId} AND status=1")
    long countTotalByUser(@Param("userId") Long userId);

    @Select("SELECT IFNULL(SUM(total_tokens),0) FROM t_call_log WHERE user_id=#{userId} AND status=1")
    long sumTotalTokensByUser(@Param("userId") Long userId);

    @Select("SELECT IFNULL(SUM(cost),0) FROM t_call_log WHERE user_id=#{userId} AND status=1")
    BigDecimal sumTotalCostByUser(@Param("userId") Long userId);

    List<ChannelStatVO> selectChannelStat(@Param("days") int days);

    List<UserStatVO> selectUserStat(@Param("days") int days);

    List<Map<String, Object>> selectUserChannelCost(@Param("userId") Long userId, @Param("days") int days);

    List<Map<String, Object>> selectUserTrend(@Param("userId") Long userId, @Param("days") int days);
}

