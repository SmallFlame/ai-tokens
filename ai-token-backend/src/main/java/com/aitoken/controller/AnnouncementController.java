package com.aitoken.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aitoken.common.Result;
import com.aitoken.entity.Announcement;
import com.aitoken.entity.User;
import com.aitoken.service.AnnouncementService;
import com.aitoken.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "公告管理")
@RestController
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    @Resource
    private UserService userService;

    /** 管理端：查询所有公告（超管） */
    @Operation(summary = "查询所有公告")
    @GetMapping("/api/admin/announcements")
    public Result<List<Announcement>> list() {
        return Result.ok(announcementService.listAll());
    }

    /** 管理端：新增公告（超管） */
    @Operation(summary = "新增公告")
    @PostMapping("/api/admin/announcements")
    public Result<Void> create(@RequestBody Announcement announcement) {
        if (announcement.getPublisher() == null || announcement.getPublisher().trim().isEmpty()) {
            Long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            String name = (user != null && user.getNickname() != null && !user.getNickname().trim().isEmpty())
                    ? user.getNickname() : (user != null ? user.getUsername() : "admin");
            announcement.setPublisher(name);
        }
        announcementService.saveAnnouncement(announcement);
        return Result.ok();
    }

    /** 管理端：更新公告（超管） */
    @Operation(summary = "更新公告")
    @PutMapping("/api/admin/announcements/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcementService.updateAnnouncement(id, announcement);
        return Result.ok();
    }

    /** 管理端：删除公告（超管） */
    @Operation(summary = "删除公告")
    @DeleteMapping("/api/admin/announcements/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.removeById(id);
        return Result.ok();
    }

    /** 用户端：获取主页公告（登录即可） */
    @Operation(summary = "获取主页展示公告")
    @GetMapping("/api/announcement/home")
    public Result<Announcement> home() {
        return Result.ok(announcementService.getHomeAnnouncement());
    }
}
