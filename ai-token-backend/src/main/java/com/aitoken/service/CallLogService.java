package com.aitoken.service;

import com.aitoken.dto.request.CallLogQuery;
import com.aitoken.dto.response.UserOverviewVO;
import com.aitoken.entity.CallLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CallLogService extends IService<CallLog> {

    void asyncSave(CallLog callLog);

    Page<CallLog> pageQuery(CallLogQuery query);

    UserOverviewVO getUserOverview(Long userId);
}
