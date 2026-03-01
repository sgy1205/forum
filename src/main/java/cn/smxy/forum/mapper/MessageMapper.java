package cn.smxy.forum.mapper;

import cn.smxy.forum.domain.entity.Message;
import cn.smxy.forum.domain.vo.ConversationListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 获取对话列表
     */
    List<ConversationListVo> getConversationList(@Param("userId") Long userId);

}
