package cn.smxy.forum.handler;

import cn.smxy.forum.domain.entity.Message;
import cn.smxy.forum.service.IMessageService;
import cn.smxy.forum.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.smxy.forum.constant.Constants.USER_STATUS_KEY;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    // 存储用户ID和WebSocketSession的映射
    private static final Map<Long, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    // 存储session和用户ID的映射
    private static final Map<WebSocketSession, Long> sessionUserMap = new ConcurrentHashMap<>();
    // 存储session的最后活跃时间
    private static final Map<WebSocketSession, Long> sessionLastActiveTime = new ConcurrentHashMap<>();
    // 存储session的心跳丢失次数
    private static final Map<WebSocketSession, AtomicInteger> sessionHeartbeatMissedCount = new ConcurrentHashMap<>();

    // 心跳配置
    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒发送一次心跳
    private static final long HEARTBEAT_TIMEOUT = 90000;  // 90秒无响应认为连接断开
    private static final int MAX_HEARTBEAT_MISS_COUNT = 3; // 最大心跳丢失次数

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis key前缀
    private static final String OFFLINE_MESSAGES_KEY = "offline:messages:";
    private static final long OFFLINE_MESSAGE_TTL = 5; // 5秒后过期

    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        // 从session中获取用户ID
        Map<String, String> params = getQueryParams(session.getUri().getQuery());
        Long userId = Long.parseLong(params.get("userId"));

        // 保存session和用户ID的映射
        sessionUserMap.put(session, userId);

        // 初始化心跳相关数据
        sessionLastActiveTime.put(session, System.currentTimeMillis());
        sessionHeartbeatMissedCount.put(session, new AtomicInteger(0));

        // 添加到用户session列表
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);

        // 设置用户在线状态
        redisUtil.setCacheObject(USER_STATUS_KEY + userId, "online", 300, java.util.concurrent.TimeUnit.SECONDS);

        logger.info("用户 {} WebSocket连接已建立, sessionId: {}", userId, session.getId());

        // 连接建立时，检查并发送离线消息
        sendOfflineMessages(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message){
        try {
            String payload = message.getPayload();

            // 检查是否是心跳消息
            if (isHeartbeatMessage(payload)) {
                handleHeartbeatResponse(session);
                return;
            }

            // 更新会话活跃时间
            updateSessionActiveTime(session);

            // 处理普通消息
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            Long senderId = chatMessage.getUserId();
            Long receiverId = chatMessage.getReceiverId();
            String content = chatMessage.getMessage();

            // 创建消息实体
            Message dbMessage = new Message();
            dbMessage.setUserId(senderId);
            dbMessage.setReceiverId(receiverId);
            dbMessage.setMessage(content);
            dbMessage.setStatus("0"); // 0-未读

            // 检查接收者是否在线
            List<WebSocketSession> receiverSessions = userSessions.get(receiverId);
            String isOnline = redisUtil.getCacheObject(USER_STATUS_KEY + receiverId);

            if (receiverSessions != null && !receiverSessions.isEmpty() && isOnline != null) {
                // 接收者在线，直接发送
                sendMessageToUser(receiverId, dbMessage);
                logger.info("用户 {} 发送消息给在线用户 {}: {}", senderId, receiverId, content);
            } else {
                // 接收者不在线，存入Redis，5秒过期
                String redisKey = OFFLINE_MESSAGES_KEY + receiverId;
                redisUtil.addToListTail(redisKey, dbMessage);
                redisUtil.expire(redisKey, OFFLINE_MESSAGE_TTL);

                logger.info("用户 {} 离线，消息已存入Redis: {}", receiverId, content);
            }

        } catch (Exception e) {
            logger.error("处理消息失败: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message){
        // 处理Pong消息（心跳响应）
        handleHeartbeatResponse(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        Long userId = sessionUserMap.get(session);

        if (userId != null) {
            // 清理心跳相关数据
            sessionLastActiveTime.remove(session);
            sessionHeartbeatMissedCount.remove(session);

            // 移除session
            List<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    // 更新用户在线状态
                    redisUtil.deleteObject(USER_STATUS_KEY + userId);
                }
            }

            // 从session映射中移除
            sessionUserMap.remove(session);

            logger.info("用户 {} WebSocket连接已关闭, 状态: {}", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception){
        logger.error("WebSocket传输错误, sessionId: {}, 错误: {}", session.getId(), exception.getMessage(), exception);

        // 发生传输错误，尝试关闭连接
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            logger.error("关闭WebSocket连接失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时发送心跳检测
     */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL)
    public void sendHeartbeats() {
        long currentTime = System.currentTimeMillis();

        for (WebSocketSession session : new CopyOnWriteArrayList<>(sessionLastActiveTime.keySet())) {
            try {
                if (session.isOpen()) {
                    // 发送Ping消息
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("PING".getBytes())));
                    logger.debug("发送心跳Ping到session: {}", session.getId());

                    // 检查是否超时
                    checkHeartbeatTimeout(session, currentTime);
                } else {
                    // 清理已关闭的连接
                    cleanupClosedSession(session);
                }
            } catch (Exception e) {
                logger.error("发送心跳失败, sessionId: {}, 错误: {}", session.getId(), e.getMessage());
                cleanupClosedSession(session);
            }
        }
    }

    /**
     * 检查心跳超时
     */
    private void checkHeartbeatTimeout(WebSocketSession session, long currentTime) {
        Long lastActiveTime = sessionLastActiveTime.get(session);
        if (lastActiveTime != null) {
            long inactiveDuration = currentTime - lastActiveTime;

            if (inactiveDuration > HEARTBEAT_TIMEOUT) {
                // 增加心跳丢失计数
                AtomicInteger missedCount = sessionHeartbeatMissedCount.get(session);
                if (missedCount != null) {
                    int count = missedCount.incrementAndGet();

                    if (count >= MAX_HEARTBEAT_MISS_COUNT) {
                        logger.warn("session {} 心跳丢失次数过多({})，关闭连接", session.getId(), count);
                        closeSessionWithHeartbeatTimeout(session);
                    } else {
                        logger.warn("session {} 心跳响应超时，丢失次数: {}", session.getId(), count);
                    }
                }
            }
        }
    }

    /**
     * 处理心跳响应
     */
    private void handleHeartbeatResponse(WebSocketSession session) {
        // 更新最后活跃时间
        updateSessionActiveTime(session);

        // 重置心跳丢失计数
        AtomicInteger missedCount = sessionHeartbeatMissedCount.get(session);
        if (missedCount != null) {
            missedCount.set(0);
        }

        logger.debug("收到session {} 的心跳响应", session.getId());
    }

    /**
     * 更新会话活跃时间
     */
    private void updateSessionActiveTime(WebSocketSession session) {
        sessionLastActiveTime.put(session, System.currentTimeMillis());
    }

    /**
     * 检查是否是心跳消息
     */
    private boolean isHeartbeatMessage(String payload) {
        try {
            Map<?, ?> message = objectMapper.readValue(payload, Map.class);
            String type = (String) message.get("type");
            return "HEARTBEAT".equals(type) || "PONG".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭心跳超时的会话
     */
    private void closeSessionWithHeartbeatTimeout(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
            }
        } catch (IOException e) {
            logger.error("关闭心跳超时的session失败: {}", e.getMessage(), e);
        } finally {
            cleanupClosedSession(session);
        }
    }

    /**
     * 清理已关闭的会话
     */
    private void cleanupClosedSession(WebSocketSession session) {
        Long userId = sessionUserMap.get(session);
        if (userId != null) {
            afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);
        }
    }

    /**
     * 发送离线消息
     */
    private void sendOfflineMessages(Long userId, WebSocketSession session) {
        try {
            String redisKey = OFFLINE_MESSAGES_KEY + userId;
            List<Message> offlineMessages = redisUtil.getCacheList(redisKey);

            if (offlineMessages != null && !offlineMessages.isEmpty()) {
                for (Message message : offlineMessages) {
                    // 发送储存到redis未保存到数据库的消息
                    sendMessage(session, message);
                }

                logger.info("用户 {} 接收到 {} 条离线消息", userId, offlineMessages.size());

                // 从Redis中移除
                redisUtil.deleteObject(redisKey);
            }
        } catch (Exception e) {
            logger.error("发送离线消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送消息给指定用户
     */
    private void sendMessageToUser(Long receiverId, Message message) {
        //存入Redis，5秒过期
        String redisKey = OFFLINE_MESSAGES_KEY + receiverId;
        redisUtil.addToListTail(redisKey, message);
        redisUtil.expire(redisKey, OFFLINE_MESSAGE_TTL);

        List<WebSocketSession> sessions = userSessions.get(receiverId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, Message message) {
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessageId(message.getMessageId());
            chatMessage.setUserId(message.getUserId());
            chatMessage.setReceiverId(message.getReceiverId());
            chatMessage.setMessage(message.getMessage());
            chatMessage.setTimestamp(message.getCreateTime());
            chatMessage.setStatus(message.getStatus());

            String json = objectMapper.writeValueAsString(chatMessage);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            logger.error("发送消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析URL参数
     */
    private Map<String, String> getQueryParams(String query) {
        Map<String, String> params = new java.util.HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }

    /**
     * 内部消息类（用于WebSocket传输）
     */
    public static class ChatMessage {
        private Long messageId;
        private Long userId;
        private Long receiverId;
        private String message;
        private String status; // 0-未读 1-已读
        private Date timestamp;

        // getters and setters
        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }

    /**
     * 强制断开用户连接
     */
    public void disconnectUser(Long userId) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    session.close();
                } catch (IOException e) {
                    logger.error("关闭用户 {} 的WebSocket连接失败", userId, e);
                }
            }
            userSessions.remove(userId);
        }
    }

    /**
     * 发送刷新未读数量通知
     * 供NotificationTask调用
     */
    public void sendRefreshNotice(Long userId) throws Exception {
        List<WebSocketSession> sessions = userSessions.get(userId);

        if (sessions != null && !sessions.isEmpty()) {
            // 构建简单通知消息
            Map<String, Object> noticeMessage = new HashMap<>();
            noticeMessage.put("type", "notice");
            noticeMessage.put("action", "refresh_unread");
            noticeMessage.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(noticeMessage);

            int sentCount = 0;
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonMessage));
                        sentCount++;
                    } catch (Exception e) {
                        logger.warn("发送刷新通知到用户 {} 失败: {}", userId, e.getMessage());
                    }
                }
            }

            logger.debug("向用户 {} 发送刷新通知，成功发送 {} 个会话", userId, sentCount);
        }
    }

}
