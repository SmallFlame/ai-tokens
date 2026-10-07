package com.aitoken.controller;

import com.aitoken.common.exception.BizException;
import com.aitoken.service.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Tag(name = "代理转发")
@RestController
public class ProxyController {

    @Resource
    private ProxyService proxyService;

    @Operation(summary = "兼容 OpenAI 的 Chat Completions 代理接口")
    @PostMapping("/v1/chat/completions")
    public void chatCompletions(HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        // 1. 提取 Authorization 头
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeError(response, 401, "Missing or invalid Authorization header");
            return;
        }
        String apiKeyValue = auth.substring(7).trim();

        // 2. 读取请求体
        String requestBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        // 3. 解析可选的渠道选择头（X-Channel-Id）
        Long channelId = parseChannelId(request.getHeader("X-Channel-Id"));

        // 4. 转发
        try {
            proxyService.forward(apiKeyValue, requestBody, response, channelId, getClientIp(request));
        } catch (BizException e) {
            if (!response.isCommitted()) {
                writeError(response, e.getCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("代理转发异常", e);
            if (!response.isCommitted()) {
                writeError(response, 500, "Proxy internal error: " + e.getMessage());
            }
        }
    }

    @Operation(summary = "兼容 Anthropic 的 Messages 代理接口（Claude Code 使用）")
    @PostMapping("/v1/messages")
    public void messages(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        // 优先读 x-api-key（标准 Anthropic 格式）
        String apiKeyValue = request.getHeader("x-api-key");

        // 兜底：Claude Code 有时以 Authorization: Bearer <token> 发送
        if (apiKeyValue == null || apiKeyValue.trim().isEmpty()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                apiKeyValue = auth.substring(7).trim();
            }
        }

        if (apiKeyValue == null || apiKeyValue.trim().isEmpty()) {
            writeError(response, 401, "Missing authentication header (x-api-key or Authorization)");
            return;
        }

        String requestBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        // 解析可选的渠道选择头（X-Channel-Id）
        Long channelId = parseChannelId(request.getHeader("X-Channel-Id"));

        try {
            proxyService.forwardAnthropic(apiKeyValue.trim(), requestBody, response, channelId, getClientIp(request));
        } catch (BizException e) {
            if (!response.isCommitted()) {
                writeError(response, e.getCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Anthropic 代理转发异常", e);
            if (!response.isCommitted()) {
                writeError(response, 500, "Proxy internal error: " + e.getMessage());
            }
        }
    }

    @Operation(summary = "兼容 OpenAI Responses 的代理接口（Codex 使用）")
    @PostMapping("/v1/responses")
    public void responses(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        // Codex 发 Authorization: Bearer <token>，兜底也读 x-api-key
        String apiKeyValue = request.getHeader("Authorization");
        if (apiKeyValue != null && apiKeyValue.startsWith("Bearer ")) {
            apiKeyValue = apiKeyValue.substring(7).trim();
        } else {
            apiKeyValue = request.getHeader("x-api-key");
        }

        if (apiKeyValue == null || apiKeyValue.trim().isEmpty()) {
            log.warn("Responses 请求缺少认证头 | UA={}", request.getHeader("User-Agent"));
            writeError(response, 401, "Missing authentication header (Authorization or x-api-key)");
            return;
        }

        String requestBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        // 解析可选的渠道选择头（X-Channel-Id）
        Long channelId = parseChannelId(request.getHeader("X-Channel-Id"));

        log.debug("Responses 请求进入 | key={} | bodyLen={} | UA={}",
                maskKey(apiKeyValue.trim()), requestBody.length(), request.getHeader("User-Agent"));

        try {
            proxyService.forwardResponses(apiKeyValue.trim(), requestBody, response, channelId, getClientIp(request));
        } catch (BizException e) {
            log.warn("Responses 业务异常 | code={} | msg={}", e.getCode(), e.getMessage());
            if (!response.isCommitted()) {
                writeError(response, e.getCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Responses 代理转发异常", e);
            if (!response.isCommitted()) {
                writeError(response, 500, "Proxy internal error: " + e.getMessage());
            }
        }
    }

    /** 只保留 Key 前 8 位，避免日志泄漏完整凭据 */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 8) + "***";
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(
                "{\"error\":{\"message\":\"" + message + "\",\"code\":" + status + "}}"
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String headerName : headerNames) {
            String value = request.getHeader(headerName);
            if (value != null && !value.trim().isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                return value.contains(",") ? value.split(",")[0].trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** 解析 X-Channel-Id 请求头，非法值返回 null */
    private Long parseChannelId(String header) {
//        if (header == null || header.isBlank()) return null;
        if (header == null || header.equals("")) return null;
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
