package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author bob
 * @since 2024-11-10
 */
@Api("我的课程相关接口")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {

    final ILearningLessonService lessonService;
    @ApiOperation("分页查询我的课表")
    @GetMapping("page")
    public PageDTO<LearningLessonVO> queryMyLesson(PageQuery pageQuery) {
        return lessonService.queryMyLesson(pageQuery);
    }

    @ApiOperation("查询正在学习的课程")
    @GetMapping("now")
    public LearningLessonVO queryCurrentLesson() {
        return lessonService.queryCurrentLesson();
    }


    @ApiOperation("检查课程是否有效")
    @GetMapping("{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId") Long courseId){
        return lessonService.isLessonValid(courseId);
    }

    @ApiOperation("查询用户课表中指定课程状态")
    @GetMapping("{courseId}")
    public LearningLessonVO  queryLessonStatus(@PathVariable("courseId") Long courseId){
        return lessonService.queryLessonStatus(courseId);
    }
    @ApiOperation("统计课程学习人数")
    @GetMapping("/lessons/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
        return lessonService.countNumByCourse(courseId);
    }

    @ApiOperation("创建学习计划")
    @PostMapping("plans")
    public void createLearningPlans(@Validated  @RequestBody LearningPlanDTO planDTO){
        lessonService.createLearningPlan(planDTO.getCourseId(),planDTO.getFreq());
    }
    @ApiOperation("查询我的学习计划")
@GetMapping("/plans")
public LearningPlanPageVO queryMyPlans(PageQuery query){
    return lessonService.queryMyPlans(query);
}




}
