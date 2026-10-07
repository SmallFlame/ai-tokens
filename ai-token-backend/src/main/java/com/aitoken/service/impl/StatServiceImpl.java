package com.aitoken.service.impl;

import com.aitoken.dto.response.DashboardVO;
import com.aitoken.dto.response.TrendVO;
import com.aitoken.entity.Channel;
import com.aitoken.mapper.CallLogMapper;
import com.aitoken.mapper.TokenStatMapper;
import com.aitoken.service.ChannelService;
import com.aitoken.service.StatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatServiceImpl implements StatService {

    @Resource
    private CallLogMapper callLogMapper;

    @Resource
    private TokenStatMapper tokenStatMapper;

    @Resource
    private ChannelService channelService;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();

        // 今日趋势
        List<TrendVO> trend = callLogMapper.selectDailyTrend(1);
        if (!trend.isEmpty()) {
            TrendVO today = trend.get(0);
            vo.setTodayCallCount(today.getCallCount());
            vo.setTodaySuccessCount(today.getSuccessCount());
            vo.setTodayTotalTokens(today.getTotalTokens());
            vo.setTodayCost(today.getTotalCost());
        } else {
            vo.setTodayCallCount(0L);
            vo.setTodaySuccessCount(0L);
            vo.setTodayTotalTokens(0L);
            vo.setTodayCost(BigDecimal.ZERO);
        }

        // 近 7 天趋势
        vo.setTrend(callLogMapper.selectDailyTrend(7));

        // 各模型分布
        List<Map<String, Object>> dist = tokenStatMapper.selectModelDistribution(30);
        vo.setModelDistribution(dist);

        // 渠道健康数
        List<Channel> allChannels = channelService.list();
        long healthCount = allChannels.stream()
                .filter(c -> c.getHealthStatus() != null && c.getHealthStatus() == 1)
                .count();
        vo.setHealthChannels((int) healthCount);
        vo.setTotalChannels(allChannels.size());

        return vo;
    }

    @Override
    public DashboardVO getDashboardForAdmin() {
        DashboardVO vo = new DashboardVO();

        List<TrendVO> trend = callLogMapper.selectDailyTrendByOutGroup(1);
        if (!trend.isEmpty()) {
            TrendVO today = trend.get(0);
            vo.setTodayCallCount(today.getCallCount());
            vo.setTodaySuccessCount(today.getSuccessCount());
            vo.setTodayTotalTokens(today.getTotalTokens());
            vo.setTodayCost(today.getTotalCost());
        } else {
            vo.setTodayCallCount(0L);
            vo.setTodaySuccessCount(0L);
            vo.setTodayTotalTokens(0L);
            vo.setTodayCost(BigDecimal.ZERO);
        }

        vo.setTrend(callLogMapper.selectDailyTrendByOutGroup(7));
        vo.setModelDistribution(tokenStatMapper.selectModelDistributionByOutGroup(30));

        // 渠道健康数（管理员也能看到）
        List<Channel> allChannels = channelService.list();
        long healthCount = allChannels.stream()
                .filter(c -> c.getHealthStatus() != null && c.getHealthStatus() == 1)
                .count();
        vo.setHealthChannels((int) healthCount);
        vo.setTotalChannels(allChannels.size());

        return vo;
    }

    @Override
    public List<TrendVO> getTrend(int days) {
        return callLogMapper.selectDailyTrend(days);
    }

    /**
     * 每小时聚合一次前一小时的数据
     * cron: 每小时第5分钟执行
     */
    @Scheduled(cron = "0 5 * * * ?")
    @Override
    public void aggregateStat() {
        // 聚合昨天的数据(全天)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();
        log.info("开始聚合统计, 日期: {}", yesterday);
        try {
            tokenStatMapper.aggregateByDate(yesterday);
            // 同时聚合今天已有数据
            tokenStatMapper.aggregateByDate(new Date());
            log.info("统计聚合完成");
        } catch (Exception e) {
            log.error("统计聚合失败", e);
        }
    }
}
