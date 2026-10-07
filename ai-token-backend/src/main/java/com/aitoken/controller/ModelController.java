package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.dto.request.ModelSaveRequest;
import com.aitoken.entity.AiModel;
import com.aitoken.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "模型管理")
@RestController
@RequestMapping("/api/admin/models")
public class ModelController {

    @Resource
    private AiModelService aiModelService;

    @Operation(summary = "新增模型")
    @PostMapping
    public Result<AiModel> save(@Validated @RequestBody ModelSaveRequest request) {
        return Result.ok(aiModelService.saveModel(request));
    }

    @Operation(summary = "编辑模型")
    @PutMapping("/{id}")
    public Result<AiModel> update(@PathVariable Long id,
                                   @Validated @RequestBody ModelSaveRequest request) {
        return Result.ok(aiModelService.updateModel(id, request));
    }

    @Operation(summary = "删除模型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        aiModelService.deleteModel(id);
        return Result.ok();
    }

    @Operation(summary = "查询模型列表")
    @GetMapping
    public Result<List<AiModel>> list(@RequestParam(required = false) Long channelId) {
        return Result.ok(aiModelService.listByChannel(channelId));
    }
}
