package com.aitoken.controller;

import cn.dev33.satoken.annotation.SaCheckDisable;
import com.aitoken.common.Result;
import com.aitoken.dto.request.LoginRequest;
import com.aitoken.dto.request.RegisterRequest;
import com.aitoken.dto.response.LoginResponse;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.User;
import com.aitoken.service.ApiKeyService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private ApiKeyService apiKeyService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }
    @Operation(summary = "查询我的 API Key 列表qt")
//    @SaCheckDisable
    @GetMapping("/qt/{username}")
    public Result<List<ApiKey>> listqt(@PathVariable String username) {

        QueryWrapper q = new QueryWrapper();
        q.eq("username",username);
        User user = userService.getOne(q);
        QueryWrapper q1 = new QueryWrapper();
        q1.eq("user_id",user.getId());
        q1.orderByDesc("created_at");
        return Result.ok(apiKeyService.list(q1));
    }
    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<User> register(@Validated @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        user.setPassword(null); // 不返回密码
        return Result.ok(user);
    }
}
