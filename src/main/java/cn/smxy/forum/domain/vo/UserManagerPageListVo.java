package cn.smxy.forum.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("用户管理分页查询数据")
public class UserManagerPageListVo {
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("用户类型 0-管理员 1-用户")
    private String userType;
    @ApiModelProperty("头像")
    private String avatar;
    @ApiModelProperty("用户名")
    private String userName;
    @ApiModelProperty("昵称")
    private String nickName;
    @ApiModelProperty("邮箱")
    private String email;
    @ApiModelProperty("积分数量")
    private Long points;
    @ApiModelProperty("禁言状态 0-未禁言 1-禁言")
    private String silenceStatus;
    @ApiModelProperty("注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;
}
