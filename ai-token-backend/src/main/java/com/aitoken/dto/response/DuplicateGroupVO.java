package com.aitoken.dto.response;

import com.aitoken.entity.User;
import lombok.Data;

import java.util.List;

/** 重复账号分组：学号或邮箱相同的用户归为一组 */
@Data
public class DuplicateGroupVO {

    /** 触发合并的字段名，如 ["studentId"] 或 ["studentId", "email"] */
    private List<String> matchedFields;

    /** 组内共同的学号（不一致时为 null） */
    private String studentId;

    /** 组内共同的邮箱（不一致时为 null） */
    private String email;

    /** 重复账号列表 */
    private List<User> users;
}
