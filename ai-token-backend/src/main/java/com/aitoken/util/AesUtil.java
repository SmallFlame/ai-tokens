package com.aitoken.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * AES 加解密工具 (用于渠道 API Key 的安全存储)
 */
@Component
public class AesUtil {

    @Value("${aitoken.aes-key}")
    private String aesKey;

    private AES getAes() {
        return SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加密为 Hex 字符串
     */
    public String encrypt(String plainText) {
        return getAes().encryptHex(plainText);
    }

    /**
     * 从 Hex 字符串解密
     */
    public String decrypt(String cipherHex) {
        return getAes().decryptStr(cipherHex);
    }
}
