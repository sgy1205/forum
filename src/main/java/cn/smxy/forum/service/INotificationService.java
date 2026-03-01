package cn.smxy.forum.service;

import cn.smxy.forum.domain.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface INotificationService extends IService<Notification> {

    /**
     * 定时添加消息列表到数据库
     */
    void addNotifications(List<Notification> notifications);

    /**
     * 获取未读消息数量
     * @param userId
     * @return
     */
    Long getTotalUnreadCount(Long userId);

}
