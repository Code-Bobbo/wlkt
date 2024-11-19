package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author bob
 * @since 2024-11-13
 */
@RestController
@RequestMapping("/learning-records")
@RequiredArgsConstructor
@Api(tags = "学习记录管理")
public class LearningRecordController {

    final ILearningRecordService recordService;
    @GetMapping("course/{courseId}")
    @ApiOperation("查询课程学习记录")
    public LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){
        return recordService.queryLearningRecordByCourse(courseId);
    }

    @PostMapping("")
    @ApiOperation("提交学习记录")
    public void submitLearningRecord(@RequestBody @Validated  LearningRecordFormDTO dto){
       recordService.addLearningRecord(dto);
    }
}
