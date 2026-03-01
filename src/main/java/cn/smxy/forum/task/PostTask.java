package cn.smxy.forum.task;

import cn.smxy.forum.domain.other.UpdatePostBrowseNumber;
import cn.smxy.forum.domain.other.UpdatePostCollectionNumber;
import cn.smxy.forum.domain.other.UpdatePostCommentNumber;
import cn.smxy.forum.domain.other.UpdatePostLikesNumber;
import cn.smxy.forum.service.IPostService;
import cn.smxy.forum.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.smxy.forum.constant.Constants.*;
@Component
public class PostTask {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IPostService postService;

    /**
     * 从redis取出需要操作的帖子点赞ID列表，储存到数据库
     */
    @Scheduled(cron = "0/10 * * * * ?")// 每10秒执行一次
    public void updatePostLikes(){
        Map<Object, Object> cacheMap = redisUtil.popHashAndDelete(REDIS_POSTLIKES_INCRCOUNTKEY);
        if(cacheMap.isEmpty()){
            return;
        }
        List<UpdatePostLikesNumber> list = cacheMap.entrySet()
                .stream()
                .map(e -> new UpdatePostLikesNumber(
                        Long.valueOf(String.valueOf(e.getKey())),
                        Long.valueOf(String.valueOf(e.getValue()))))
                .collect(Collectors.toList());

        postService.updatePostLikesNumber(list);
    }

    /**
     * 从redis取出需要操作的帖子收藏ID列表，储存到数据库
     */
    @Scheduled(cron = "0/10 * * * * ?")// 每10秒执行一次
    public void updatePostCollection(){
        Map<Object, Object> cacheMap = redisUtil.popHashAndDelete(REDIS_POSTCOLLECTION_INCRCOUNTKEY);
        if(cacheMap.isEmpty()){
            return;
        }
        List<UpdatePostCollectionNumber> list = cacheMap.entrySet()
                .stream()
                .map(e -> new UpdatePostCollectionNumber(
                        Long.valueOf(String.valueOf(e.getKey())),
                        Long.valueOf(String.valueOf(e.getValue()))))
                .collect(Collectors.toList());

        postService.updatePostCollectionNumber(list);
    }

    /**
     * 从redis取出需要操作的帖子评论ID列表，储存到数据库
     */
    @Scheduled(cron = "0/10 * * * * ?")// 每10秒执行一次
    public void updatePostComment(){
        Map<Object, Object> cacheMap = redisUtil.popHashAndDelete(REDIS_POSTCOMMENT_INCRCOUNTKEY);
        if(cacheMap.isEmpty()){
            return;
        }
        List<UpdatePostCommentNumber> list = cacheMap.entrySet()
                .stream()
                .map(e -> new UpdatePostCommentNumber(
                        Long.valueOf(String.valueOf(e.getKey())),
                        Long.valueOf(String.valueOf(e.getValue()))))
                .collect(Collectors.toList());

        postService.updatePostCommentNumber(list);
    }


    /**
     * 从redis取出需要操作的帖子浏览量ID列表，储存到数据库
     */
    @Scheduled(cron = "0/10 * * * * ?")// 每10秒执行一次
    public void updatePostBrowse(){
        Map<Object, Object> cacheMap = redisUtil.popHashAndDelete(REDIS_POSTBROWSE_INCRCOUNTKEY);
        if(cacheMap.isEmpty()){
            return;
        }
        List<UpdatePostBrowseNumber> list = cacheMap.entrySet()
                .stream()
                .map(e -> new UpdatePostBrowseNumber(
                        Long.valueOf(String.valueOf(e.getKey())),
                        Long.valueOf(String.valueOf(e.getValue()))))
                .collect(Collectors.toList());

        postService.updatePostBrowseNumber(list);
    }
}
