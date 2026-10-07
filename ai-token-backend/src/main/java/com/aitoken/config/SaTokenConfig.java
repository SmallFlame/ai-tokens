package com.aitoken.config;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由拦截 + CORS 配置
 *
 * 角色体系（code 字符串）：
 * - super_admin: 超级管理员（所有权限）
 * - advisor: 导师（用户管理、小组管理、查看全部数据）
 * - leader: 组长（团队管理、查本组数据）
 * - member: 组员（基础权限）
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // /api/auth/** 和 /api/keys/qt/** 不拦截 (登录/注册 + Qt 客户端获取 Keys)
            SaRouter.match("/api/**")
                    .notMatch("/api/auth/**", "/api/keys/qt/**")
                    .check(r -> StpUtil.checkLogin());

            // /api/logs/** 仅超管可访问
            SaRouter.match("/api/logs/**")
                    .check(r -> checkSuperAdmin());

            // /api/admin/keys、渠道、模型 仅超管可访问
            SaRouter.match("/api/admin/keys/**",
                           "/api/admin/channels/**", "/api/admin/channels",
                           "/api/admin/models/**",   "/api/admin/models")
                    .check(r -> checkSuperAdmin());

            // 超管和导师均可访问：小组管理、管理员用户管理、充值日志
            SaRouter.match("/api/admin/groups/**", "/api/admin/admin-users/**",
                           "/api/admin/recharge-logs/**")
                    .check(r -> checkAdminOrSuperAdmin());

            // 角色管理 仅超管可访问
            SaRouter.match("/api/admin/roles/**")
                    .check(r -> checkSuperAdmin());

            // 其他 /api/admin/**（含用户管理）仅超管可访问
            SaRouter.match("/api/admin/**")
                    .notMatch("/api/admin/groups/**", "/api/admin/admin-users/**",
                              "/api/admin/roles/**", "/api/admin/recharge-logs/**")
                    .check(r -> checkSuperAdmin());

            // /api/team/** 组长、导师及超管可访问
            SaRouter.match("/api/team/**")
                    .check(r -> checkLeaderOrAdmin());
        })).addPathPatterns("/**");
    }

    /**
     * 检查是否是超级管理员 (roleCode = "super_admin")
     */
    private void checkSuperAdmin() {
        String roleCode = (String) StpUtil.getSession().get("roleCode");
        if (roleCode == null || !"super_admin".equals(roleCode)) {
            throw new SaTokenException("仅超级管理员可访问");
        }
    }

    /**
     * 检查是否是导师或超级管理员 (roleCode = "super_admin" 或 "advisor")
     */
    private void checkAdminOrSuperAdmin() {
        String roleCode = (String) StpUtil.getSession().get("roleCode");
        if (roleCode == null || (!"super_admin".equals(roleCode) && !"advisor".equals(roleCode))) {
            throw new SaTokenException("导师及以上权限可访问");
        }
    }

    /**
     * 检查是否是组长、导师或超级管理员 (roleCode = "super_admin" / "advisor" / "leader")
     */
    private void checkLeaderOrAdmin() {
        String roleCode = (String) StpUtil.getSession().get("roleCode");
        if (roleCode == null || (!"super_admin".equals(roleCode)
                && !"advisor".equals(roleCode)
                && !"leader".equals(roleCode))) {
            throw new SaTokenException("组长及以上权限可访问");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

