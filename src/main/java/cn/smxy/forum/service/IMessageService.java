package cn.smxy.forum.service;

import cn.smxy.forum.domain.entity.Message;
import cn.smxy.forum.domain.vo.ConversationListVo;
import cn.smxy.forum.utils.R;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IMessageService extends IService<Message> {

    /**
     * 获取未读的聊天记录
     */
    R<List<Message>> getChatHistory(Long userId, Long receiverId);


    /**
     * 批量标记为已读
     */
    R<Boolean> markAllAsRead(Long userId, Long receiverId);

    /**
     * 获取已读的30天内的历史消息
     */
    List<Message> getOldHistory(Long userId, Long receiverId);

    /**
     * 获取对话列表
     */
    List<ConversationListVo> getConversationList(Long userId);
}
