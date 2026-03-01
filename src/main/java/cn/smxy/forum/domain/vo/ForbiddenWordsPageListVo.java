package cn.smxy.forum.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("查询违禁词列表")
public class ForbiddenWordsPageListVo {

  @ApiModelProperty(value = "违禁词id")
  private Long forbiddenWordsId;

  @ApiModelProperty(value = "类型（0-词组 1-正则）")
  private Integer type;

  @ApiModelProperty(value = "违禁词")
  private String forbiddenWord;

  @ApiModelProperty(value = "备注")
  private String remark;

  @ApiModelProperty(value = "创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
  private Date createTime;

}
