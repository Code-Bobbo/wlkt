package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author bob
 * @since 2024-12-04
 */
@RestController
@Api(tags = "互动提问的问题相关管理端接口")
@RequestMapping("admin/questions")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {
    private final IInteractionQuestionService questionService;
    @ApiOperation("分页查询问题列表-管理端")
    @GetMapping("page")
    public PageDTO<QuestionAdminVO> queryQuestionAdminPage(QuestionAdminPageQuery query){
        return questionService.queryQuestionAdminPage(query);
    }

}
