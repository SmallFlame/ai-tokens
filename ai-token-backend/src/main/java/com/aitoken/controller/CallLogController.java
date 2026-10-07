package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.dto.request.CallLogQuery;
import com.aitoken.entity.CallLog;
import com.aitoken.service.CallLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "调用日志")
@RestController
@RequestMapping("/api/logs")
public class CallLogController {

    @Resource
    private CallLogService callLogService;

    @Operation(summary = "分页查询调用日志")
    @GetMapping
    public Result<Page<CallLog>> page(CallLogQuery query) {
        return Result.ok(callLogService.pageQuery(query));
    }

    @Operation(summary = "查询单条日志详情")
    @GetMapping("/{id}")
    public Result<CallLog> detail(@PathVariable Long id) {
        return Result.ok(callLogService.getById(id));
    }
}
