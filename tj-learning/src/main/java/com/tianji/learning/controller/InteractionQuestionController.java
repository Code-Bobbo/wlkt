package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
@Api(tags = "互动提问的问题相关接口")
@RequestMapping("/questions")
@RequiredArgsConstructor
public class InteractionQuestionController {
    private final IInteractionQuestionService questionService;
    @ApiOperation("新增互动问题")
    @PostMapping
    public void saveQuestion(@RequestBody @Validated QuestionFormDTO dto){
        questionService.save(dto);
    }

    @ApiOperation("修改互动问题")
    @PutMapping("{id}")
    public void updateQuestion(@PathVariable Long id, @RequestBody QuestionFormDTO dto){
        questionService.updateQuestion(id,dto);
    }

    @ApiOperation("分页查询互动问题 --用户端")
    @GetMapping("page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
       return questionService.queryQuestionPage(query);
    }
    @ApiOperation("根据id查询互动问题详情")
    @GetMapping("{id}")
    public QuestionVO getQuestionById(@PathVariable Long id){
        return questionService.getQuestionById(id);
    }


}
