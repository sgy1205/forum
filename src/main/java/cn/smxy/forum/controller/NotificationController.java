package cn.smxy.forum.controller;

import cn.smxy.forum.domain.entity.Notification;
import cn.smxy.forum.domain.entity.Points;
import cn.smxy.forum.domain.other.BaseEntity;
import cn.smxy.forum.domain.other.TableDataInfo;
import cn.smxy.forum.domain.param.other.PageQuery;
import cn.smxy.forum.domain.vo.NotificationListVo;
import cn.smxy.forum.mapping.NotificationMapping;
import cn.smxy.forum.service.INotificationService;
import cn.smxy.forum.utils.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.smxy.forum.constant.Constants.*;

@RestController
@RequestMapping("/notification")
@Api(tags = "消息通知模块")
public class NotificationController extends BaseController {

    @Autowired
    private INotificationService notificationService;

    @GetMapping("/list")
    @ApiOperation("获取当前登录用户的消息列表")
    public TableDataInfo<List<NotificationListVo>> notificationList(@Validated PageQuery pageQuery,String type) {
        Page<Notification> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Notification> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Notification::getDelFlag,NO_DELETE)
                .eq(Notification::getUserId,getUserId())
                .eq(type!=null,Notification::getType,type).orderByDesc(BaseEntity::getCreateTime);
        Page<Notification> notifications = notificationService.page(page,lqw);

        List<NotificationListVo> listVos= NotificationMapping.INSTANCE.toListVoList(notifications.getRecords());
        return getDataTable(listVos, notifications.getTotal());
    }

    @PostMapping("/readAll")
    @ApiOperation("将所有未读消息改为已读")
    public R readAllNotification() {
        LambdaUpdateWrapper<Notification> luw = new LambdaUpdateWrapper<>();
        luw.set(Notification::getReadStatus, 1L)
                .eq(Notification::getDelFlag,NO_DELETE)
                .eq(Notification::getUserId,getUserId())
                .eq(Notification::getReadStatus,0);

        return R.to(notificationService.update(luw), "修改");
    }

    @DeleteMapping()
    @ApiOperation("删除消息")
    public R deleteNotifications(@RequestBody List<Long> notificationIds) {
        LambdaUpdateWrapper<Notification> luw = new LambdaUpdateWrapper<>();
        luw.set(Notification::getDelFlag,DELETE)
                .in(Notification::getNotificationId,notificationIds);

        return R.to(notificationService.update(luw), "删除");
    }

    @GetMapping("/unreadNotificationNum")
    @ApiOperation("获取未读消息数量")
    public R getUnreadNotificationNum() {
        long count = notificationService.getTotalUnreadCount(getUserId());
        return R.ok(count);
    }

    @PutMapping("/{notificationId}")
    @ApiOperation("将单条消息状态改为已读")
    public R updateNotificationStatus(@PathVariable("notificationId") Long notificationId) {
        LambdaUpdateWrapper<Notification> luw = new LambdaUpdateWrapper<>();
        luw.set(Notification::getReadStatus,1).eq(Notification::getNotificationId,notificationId);
        return R.to(notificationService.update(luw),"修改");
    }
}
