package cn.smxy.forum.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("当前登录用户的消息列表数据")
public class NotificationListVo {
    @ApiModelProperty("消息ID")
    private Long notificationId;
    @ApiModelProperty("接收通知用户ID")
    private Long userId;
    @ApiModelProperty("消息类型 0-系统通知 1-操作通知")
    private String type;
    @ApiModelProperty("消息内容")
    private String message;
    @ApiModelProperty("关联ID")
    private Long relatedId;
    @ApiModelProperty("关联ID类型 0-用户 1-帖子 2-评论")
    private String relatedType;
    @ApiModelProperty("当关联ID类型为评论时的帖子ID")
    private Long carrierId;
    @ApiModelProperty("阅读状态 0-未读 1-已读")
    private Long readStatus;
    @ApiModelProperty("接收时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;
    @ApiModelProperty("消息发出者ID")
    private Long operatorId;
    @ApiModelProperty("消息发出者头像")
    private String avatar;
}
