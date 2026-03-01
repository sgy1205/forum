package cn.smxy.forum.task;

import cn.smxy.forum.domain.entity.Notification;
import cn.smxy.forum.handler.WebSocketHandler;
import cn.smxy.forum.service.INotificationService;
import cn.smxy.forum.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.smxy.forum.constant.Constants.REDIS_NOTIFICATIONS_KEY;
import static cn.smxy.forum.constant.Constants.USER_STATUS_KEY;

@Component
public class NotificationTask {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private INotificationService notificationService;
    @Autowired
    private WebSocketHandler webSocketHandler;

    private static final Logger logger = LoggerFactory.getLogger(NotificationTask.class);

    /**
     * 从redis取出保存的消息列表，储存到数据库
     */
    @Scheduled(cron = "0/10 * * * * ?")// 每10秒执行一次
    public void checkNotifications() {
        List<Notification> notifications = redisUtil.popFromList(REDIS_NOTIFICATIONS_KEY,300,Notification.class);
        if (notifications != null && !notifications.isEmpty()) {
            notificationService.addNotifications(notifications);

            // 2. 获取需要通知的用户ID（去重）
            Set<Long> userIds = notifications.stream()
                    .map(Notification::getUserId)
                    .collect(Collectors.toSet());

            // 3. 通知在线用户刷新未读数量
            this.notifyOnlineUsers(userIds);
        }
    }

    /**
     * 通知在线用户刷新未读数量
     */
    public void notifyOnlineUsers(Set<Long> userIds) {
        int notifiedCount = 0;

        for (Long userId : userIds) {
            try {
                // 检查用户是否在线
                String isOnline = redisUtil.getCacheObject(USER_STATUS_KEY + userId);

                if (isOnline != null) {
                    // 发送WebSocket通知
                    webSocketHandler.sendRefreshNotice(userId);
                    notifiedCount++;
                }
            } catch (Exception e) {
                logger.error("通知用户 {} 失败: {}", userId, e.getMessage());
            }
        }
        logger.info("成功通知 {} 个在线用户刷新未读数量", notifiedCount);
    }

}
