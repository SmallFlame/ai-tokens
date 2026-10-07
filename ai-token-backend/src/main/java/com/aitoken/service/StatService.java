package com.aitoken.service;

import com.aitoken.dto.response.DashboardVO;
import com.aitoken.dto.response.TrendVO;

import java.util.List;

public interface StatService {

    DashboardVO getDashboard();

    /** 管理员专用看板：仅统计组外用户数据 */
    DashboardVO getDashboardForAdmin();

    List<TrendVO> getTrend(int days);

    /**
     * 定时聚合统计任务 (每小时执行一次)
     */
    void aggregateStat();
}
