package cn.smxy.forum.controller;

import cn.smxy.forum.domain.entity.Comment;
import cn.smxy.forum.domain.param.insert.AddCommentDTO;
import cn.smxy.forum.domain.vo.CommentListVo;
import cn.smxy.forum.domain.vo.ForbiddenWordCheckResultVo;
import cn.smxy.forum.service.ICommentService;
import cn.smxy.forum.service.IForbiddenWordsCheckService;
import cn.smxy.forum.utils.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
@Api(tags = "评论模块")
public class CommentController extends BaseController {

    @Autowired
    private ICommentService commentService;
    @Autowired
    private IForbiddenWordsCheckService forbiddenWordsCheckService;

    @PostMapping()
    @ApiOperation("添加评论")
    public R addComment(@RequestBody AddCommentDTO commentDTO) {
        String text =commentDTO.getCommentContent().replaceAll("<[^>]+>", "");
        ForbiddenWordCheckResultVo resultVo = forbiddenWordsCheckService.checkForbiddenWords(text);
        if(!resultVo.isResult()){
            return R.fail("你的评论存在违禁词："+resultVo.getForbiddenWords());
        }
        return R.to(commentService.insertComment(commentDTO,getUserId()),"评论");
    }

    @DeleteMapping("/{commentId}")
    @ApiOperation("删除评论")
    public R deleteComment(@PathVariable("commentId") Long commentId) {
        Comment comment = commentService.getById(commentId);
        if(!comment.getUserId().equals(getUserId())){
            return R.fail("你无权删除他人评论");
        }else {
            return R.to(commentService.deleteComment(comment),"删除");
        }
    }

    @GetMapping("/list/{postId}")
    @ApiOperation("获取评论区评论列表")
    public R<List<CommentListVo>> getCommentList(@PathVariable("postId") Long postId) {
        return R.ok(commentService.getCommentTree(postId,getUserId()));
    }
}
