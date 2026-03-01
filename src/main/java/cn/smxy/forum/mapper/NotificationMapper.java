package cn.smxy.forum.mapper;

import cn.smxy.forum.domain.entity.Notification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface NotificationMapper extends BaseMapper<Notification> {

    Long getTotalUnreadCount(@Param("userId") Long userId);

}
