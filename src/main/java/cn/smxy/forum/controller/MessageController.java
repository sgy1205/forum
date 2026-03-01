package cn.smxy.forum.controller;


import cn.smxy.forum.domain.entity.Message;
import cn.smxy.forum.domain.vo.ConversationListVo;
import cn.smxy.forum.service.IMessageService;
import cn.smxy.forum.utils.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@Api(tags = "私信模块")
public class MessageController extends BaseController{

    @Autowired
    private IMessageService messageService;

    @GetMapping("/history")
    @ApiOperation("获取未读的聊天记录")
    public R<List<Message>> getChatHistory(
            @Parameter(description = "当前用户ID") @RequestParam Long userId,
            @Parameter(description = "对方用户ID") @RequestParam Long receiverId) {
        return messageService.getChatHistory(userId, receiverId);
    }

    @GetMapping("/read/history")
    @ApiOperation("获取已读的30天内的聊天消息")
    public R<List<Message>> getReadHistory(
            @Parameter(description = "当前用户ID") @RequestParam Long userId,
            @Parameter(description = "对方用户ID") @RequestParam Long receiverId) {
        return R.ok(messageService.getOldHistory(userId, receiverId));
    }

    @PutMapping("/read/all")
    @ApiOperation("批量标记为已读")
    public R<Boolean> markAllAsRead(@Parameter(description = "当前用户ID") @RequestParam Long userId,
                                    @Parameter(description = "对方用户ID") @RequestParam Long receiverId) {
        return messageService.markAllAsRead(userId, receiverId);
    }

    @GetMapping("/list")
    @ApiOperation("获取对话列表")
    public R<List<ConversationListVo>> getConversationList() {
        return R.ok(messageService.getConversationList(getUserId()));
    }

}
