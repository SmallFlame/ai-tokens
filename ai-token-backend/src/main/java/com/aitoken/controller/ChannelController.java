package com.aitoken.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aitoken.common.Result;
import com.aitoken.dto.request.ChannelSaveRequest;
import com.aitoken.entity.Channel;
import com.aitoken.entity.ChannelRecharge;
import com.aitoken.entity.SysConfig;
import com.aitoken.mapper.ChannelMapper;
import com.aitoken.mapper.ChannelRechargeMapper;
import com.aitoken.mapper.SysConfigMapper;
import com.aitoken.service.ChannelService;
import com.aitoken.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "渠道管理")
@RestController
@RequestMapping("/api/admin/channels")
public class ChannelController {

    @Resource
    private ChannelService channelService;

    @Resource
    private AesUtil aesUtil;

    @Resource
    private ChannelMapper channelMapper;

    @Resource
    private ChannelRechargeMapper channelRechargeMapper;

    @Resource
    private SysConfigMapper sysConfigMapper;

    private static final String DEFAULT_CHANNEL_KEY = "default_channel_id";

    @Operation(summary = "新增渠道")
    @PostMapping
    public Result<Channel> save(@Validated @RequestBody ChannelSaveRequest request) {
        Channel channel = channelService.saveChannel(request);
        channel.setApiKey(null); // 不返回加密 Key
        return Result.ok(channel);
    }

    @Operation(summary = "编辑渠道")
    @PutMapping("/{id}")
    public Result<Channel> update(@PathVariable Long id,
                                   @Validated @RequestBody ChannelSaveRequest request) {
        Channel channel = channelService.updateChannel(id, request);
        channel.setApiKey(null);
        return Result.ok(channel);
    }

    @Operation(summary = "删除渠道")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return Result.ok();
    }

    @Operation(summary = "查询渠道列表")
    @GetMapping
    public Result<List<Channel>> list() {
        List<Channel> list = channelService.list();
        list.forEach(c -> c.setApiKey(null)); // 脱敏
        return Result.ok(list);
    }

    @Operation(summary = "触发健康检测")
    @PostMapping("/{id}/health-check")
    public Result<Boolean> healthCheck(@PathVariable Long id) {
        return Result.ok(channelService.healthCheck(id));
    }

    @Operation(summary = "更新渠道状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        channelService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "获取默认渠道ID")
    @GetMapping("/default")
    public Result<Long> getDefault() {
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, DEFAULT_CHANNEL_KEY));
        if (cfg == null || cfg.getConfigValue() == null) return Result.ok(null);
        try { return Result.ok(Long.parseLong(cfg.getConfigValue())); }
        catch (NumberFormatException e) { return Result.ok(null); }
    }

    @Operation(summary = "设置默认渠道ID（null 清除默认）")
    @PutMapping("/default")
    public Result<Void> setDefault(@RequestParam(required = false) Long channelId) {
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, DEFAULT_CHANNEL_KEY));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(DEFAULT_CHANNEL_KEY);
            cfg.setDescription("默认渠道ID，新建Key时预填此渠道");
            cfg.setConfigValue(channelId == null ? null : String.valueOf(channelId));
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(channelId == null ? null : String.valueOf(channelId));
            sysConfigMapper.updateById(cfg);
        }
        return Result.ok();
    }

    @Operation(summary = "渠道充值/扣减预算（delta>0 充值，<0 扣减）")
    @PostMapping("/{id}/recharge")
    public Result<Void> recharge(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body.get("delta") == null) return Result.fail("缺少 delta 参数");
        BigDecimal delta = new BigDecimal(body.get("delta").toString());
        String remark = body.get("remark") != null ? body.get("remark").toString() : null;
        Long operatorId = StpUtil.getLoginIdAsLong();

        channelMapper.rechargeTotal(id, delta);

        ChannelRecharge log = new ChannelRecharge();
        log.setChannelId(id);
        log.setDelta(delta);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        channelRechargeMapper.insert(log);
        return Result.ok();
    }

    @Operation(summary = "查询渠道充值日志")
    @GetMapping("/{id}/recharge-logs")
    public Result<List<ChannelRecharge>> rechargeLogs(@PathVariable Long id) {
        List<ChannelRecharge> logs = channelRechargeMapper.selectList(
                new LambdaQueryWrapper<ChannelRecharge>()
                        .eq(ChannelRecharge::getChannelId, id)
                        .orderByDesc(ChannelRecharge::getCreatedAt));
        return Result.ok(logs);
    }
}
