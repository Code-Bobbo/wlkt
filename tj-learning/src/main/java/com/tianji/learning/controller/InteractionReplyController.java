package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author bob
 * @since 2024-12-04
 */
@Api(tags = "互动提问的回答或评论管理接口")
@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class InteractionReplyController {
    private final IInteractionReplyService replyService;
    @PostMapping("")
    @ApiOperation("新增互动提问的回答或评论")
    public void saveReply(@RequestBody ReplyDTO reply){
        replyService.saveReply(reply);
    }
    @GetMapping("page")
    @ApiOperation("分页查询互动提问的回答或评论")
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query){
        return replyService.queryReplyPage(query);
    }
}
