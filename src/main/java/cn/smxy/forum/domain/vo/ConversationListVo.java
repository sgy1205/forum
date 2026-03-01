package cn.smxy.forum.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("私信消息列表")
public class ConversationListVo {
    @ApiModelProperty("对话用户ID")
    private Long userId;
    @ApiModelProperty("对话用户昵称")
    private String nickName;
    @ApiModelProperty("最后一条对话内容")
    private String message;
    @ApiModelProperty("自己的未读消息的数量")
    private Integer unreadCount;
    @ApiModelProperty("对话用户头像")
    private String avatar;
    @ApiModelProperty("对话时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;
}
