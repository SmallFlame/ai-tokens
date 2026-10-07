package com.aitoken.service.impl;

import com.aitoken.entity.Announcement;
import com.aitoken.mapper.AnnouncementMapper;
import com.aitoken.service.AnnouncementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {

    @Override
    public List<Announcement> listAll() {
        return list(new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreatedAt));
    }

    @Override
    public Announcement getHomeAnnouncement() {
        return getOne(new LambdaQueryWrapper<Announcement>().eq(Announcement::getShowOnHome, 1).last("LIMIT 1"));
    }

    @Override
    @Transactional
    public void saveAnnouncement(Announcement announcement) {
        if (Integer.valueOf(1).equals(announcement.getShowOnHome())) {
            // 先清除其他主页公告
            lambdaUpdate().set(Announcement::getShowOnHome, 0).update();
        }
        save(announcement);
    }

    @Override
    @Transactional
    public void updateAnnouncement(Long id, Announcement announcement) {
        if (Integer.valueOf(1).equals(announcement.getShowOnHome())) {
            lambdaUpdate().set(Announcement::getShowOnHome, 0).update();
        }
        announcement.setId(id);
        updateById(announcement);
    }
}
