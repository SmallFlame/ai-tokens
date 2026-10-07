package com.aitoken.service;

import com.aitoken.entity.Announcement;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface AnnouncementService extends IService<Announcement> {

    List<Announcement> listAll();

    Announcement getHomeAnnouncement();

    void saveAnnouncement(Announcement announcement);

    void updateAnnouncement(Long id, Announcement announcement);
}
