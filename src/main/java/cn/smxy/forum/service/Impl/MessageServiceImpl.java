package cn.smxy.forum.service.Impl;

import cn.smxy.forum.domain.entity.Message;
import cn.smxy.forum.domain.vo.ConversationListVo;
import cn.smxy.forum.mapper.MessageMapper;
import cn.smxy.forum.service.IMessageService;
import cn.smxy.forum.task.NotificationTask;
import cn.smxy.forum.utils.R;
import cn.smxy.forum.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private NotificationTask notificationTask;

    // Redis key前缀
    private static final String OFFLINE_MESSAGES_KEY = "offline:messages:";

    @Override
    public R<List<Message>> getChatHistory(Long userId, Long receiverId) {
        try {
            QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "0").and(wrapper -> wrapper
                            .eq("user_id", userId).eq("receiver_id", receiverId)
                            .or()
                            .eq("user_id", receiverId).eq("receiver_id", userId))
                    .orderByAsc("create_time");

            List<Message> resultPage = messageMapper.selectList(queryWrapper);

            return R.ok(resultPage);
        } catch (Exception e) {
            logger.error("获取聊天记录失败: {}", e.getMessage(), e);
            return R.fail("获取聊天记录失败");
        }
    }

    @Override
    public R<Boolean> markAllAsRead(Long userId, Long receiverId) {
        try {
            Message updateMessage = new Message();
            updateMessage.setStatus("1");

            QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", receiverId)
                    .eq("receiver_id", userId)
                    .eq("status", "0");

            boolean updated = update(updateMessage, queryWrapper);
            return R.ok(updated);
        } catch (Exception e) {
            logger.error("批量标记消息为已读失败: {}", e.getMessage(), e);
            return R.fail("批量标记消息为已读失败");
        }
    }

    @Override
    public List<Message> getOldHistory(Long userId, Long receiverId) {
        // 直接计算30天前的时间
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);

        LambdaQueryWrapper<Message> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Message::getStatus, "1")
                .and(i -> i.eq(Message::getUserId, userId).eq(Message::getReceiverId, receiverId)
                        .or()
                        .eq(Message::getUserId, receiverId).eq(Message::getReceiverId, userId))
                .ge(Message::getCreateTime, thirtyDaysAgo)
                .orderByAsc(Message::getCreateTime);
        return messageMapper.selectList(lqw);
    }

    @Override
    public List<ConversationListVo> getConversationList(Long userId) {
        return messageMapper.getConversationList(userId);
    }

    /**
     * 定时任务：处理Redis中缓存的消息
     * 每5秒执行一次
     */
    @Scheduled(fixedDelay = 5 * 1000) // 5秒
    public void processExpiredOfflineMessages() {
        logger.info("开始保存消息到数据库");

        try {
            // 查找所有需要保存的消息的key
            String pattern = OFFLINE_MESSAGES_KEY + "*";
            redisUtil.keys(pattern).forEach(key -> {
                try {
                    // 获取消息列表
                    List<Message> messages = redisUtil.getCacheList(key);
                    if (messages != null && !messages.isEmpty()) {
                        saveBatch(messages);

                        Set<Long> userIds =messages.stream()
                                .map(Message::getReceiverId)
                                .collect(Collectors.toSet());

                        notificationTask.notifyOnlineUsers(userIds);

                        logger.info("已将 {} 条消息批量保存到数据库，key: {}", messages.size(), key);

                        // 从Redis中删除
                        redisUtil.deleteObject(key);
                    }
                } catch (Exception e) {
                    logger.error("保存消息失败，key: {}", key, e);
                }
            });
        } catch (Exception e) {
            logger.error("定时任务执行失败: {}", e.getMessage(), e);
        }

        logger.info("过期离线消息处理完成");
    }

}
