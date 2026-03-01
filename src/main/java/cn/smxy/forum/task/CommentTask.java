package cn.smxy.forum.task;

import cn.smxy.forum.domain.other.UpdateCommentLikesNumber;
import cn.smxy.forum.domain.other.UpdatePostLikesNumber;
import cn.smxy.forum.service.ICommentService;
import cn.smxy.forum.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.smxy.forum.constant.Constants.*;

@Component
public class CommentTask {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ICommentService commentService;

    /**
     * 从redis取出需要操作的评论点赞ID列表，储存到数据库
     */
    @Scheduled(cron = "0/10 * * * * ?")// 每10秒执行一次
    public void updateCommentLikes(){
        Map<Object, Object> cacheMap = redisUtil.popHashAndDelete(REDIS_COMMENTLIKES_INCRCOUNTKEY);
        if(cacheMap.isEmpty()){
            return;
        }
        List<UpdateCommentLikesNumber> list = cacheMap.entrySet()
                .stream()
                .map(e -> new UpdateCommentLikesNumber(
                        Long.valueOf(String.valueOf(e.getKey())),
                        Long.valueOf(String.valueOf(e.getValue()))))
                .collect(Collectors.toList());

        commentService.updateCommentLikesNumber(list);
    }

}
