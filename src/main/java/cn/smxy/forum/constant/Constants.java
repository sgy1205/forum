package cn.smxy.forum.constant;

/**
 * 通用常量信息
 *
 * @author ruoyi
 */
public class Constants {
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * www主域
     */
    public static final String WWW = "www.";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 默认用户名常量
     */
    public static final String DEFAULT_USER = "system"; // 可根据实际情况调整

    /**
     * 删除标志常量-删除
     */
    public static final String DELETE = "2";

    /**
     * 删除标志常量-恢复/未删除
     */
    public static final String NO_DELETE = "0";

    /**
     * 消息相关类型-用户
     */
    public static final String RELATED_TYPE_USER = "0";

    /**
     * 消息相关类型-帖子
     */
    public static final String RELATED_TYPE_POST = "1";

    /**
     * 消息相关类型-评论
     */
    public static final String RELATED_TYPE_COMMENT = "2";

    /**
     * 修改帖子收藏状态的redis的key
     */
    public static final String REDIS_UPDATECOLLECTION_KEY = "updateCollections";

    /**
     * 消息提醒的redis的key
     */
    public static final String REDIS_NOTIFICATIONS_KEY = "notifications";

    /**
     * 修改点赞状态的redis的key
     */
    public static final String REDIS_LIKES_KEY = "likes";

    /**
     * 修改帖子点赞数的redis的key
     */
    public static final String REDIS_POSTLIKES_INCRCOUNTKEY = "postLikesIncrCount";

    /**
     * 修改帖子收藏数量的redis的key
     */
    public static final String REDIS_POSTCOLLECTION_INCRCOUNTKEY="postCollectionIncrCount";

    /**
     * 修改帖子浏览量的redis的key
     */
    public static final String REDIS_POSTBROWSE_INCRCOUNTKEY="postBrowseIncrCount";

    /**
     * 修改帖子评论数的redis的key
     */
    public static final String REDIS_POSTCOMMENT_INCRCOUNTKEY="posstCommentIncrCount";

    /**
     * 修改评论点赞数的redis的key
     */
    public static final String REDIS_COMMENTLIKES_INCRCOUNTKEY = "commentLikesIncrCount";

    /**
     * 用户ws连接状态的key
     */
    public static final String USER_STATUS_KEY = "user:online:";
}
