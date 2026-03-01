package cn.smxy.forum.domain.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("查询违禁词详情违禁词参数")
public class ForbiddenWordsDetailVo {

  @ApiModelProperty(value = "类型（0-词组 1-正则）")
  private Long type;

  @ApiModelProperty(value = "违禁词")
  private String forbiddenWord;

  @ApiModelProperty(value = "备注")
  private String remark;

  @ApiModelProperty(value = "状态（0-发布 1-未发布）")
  private String status;

  @ApiModelProperty(value = "创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
  private Date createTime;

  @ApiModelProperty(value = "创建者")
  private String createBy;

  @ApiModelProperty(value = "更新时间")
  private Date updateTime;

  @ApiModelProperty(value = "更新者")
  private String updateBy;

  @ApiModelProperty(value = "删除标志（0-正常 2-删除）")
  @TableLogic//逻辑删除的标识
  private String delFlag;

}
