package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author bob
 * @since 2024-11-10
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    final CourseClient courseClient;
    final CatalogueClient catalogueClient;
    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        //1. 通过feign 远程调用课程服务，获取课程信息
        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(courseIds);
        //2. 封装实体类
        List<LearningLesson> learningLessons = new ArrayList<>();

        for (CourseSimpleInfoDTO cinfo : cinfos) {
            LearningLesson learningLesson = new LearningLesson();
            learningLesson.setUserId(userId);
            learningLesson.setCourseId(cinfo.getId());
            Integer validDuration = cinfo.getValidDuration(); //课程有效期 单位是月
            if(validDuration !=null){
                LocalDateTime now = LocalDateTime.now();
                learningLesson.setCreateTime(now);
                learningLesson.setExpireTime(now.plusMonths(validDuration));
            }
            learningLessons.add(learningLesson);
        }
        //3. 批量保存
        this.saveBatch(learningLessons);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLesson(PageQuery pageQuery) {
        //1.获取当前登陆人
        Long userId = UserContext.getUser();
        //2. 分页查询我的课表
        Page<LearningLesson> page = this.lambdaQuery().eq(LearningLesson::getUserId, userId).page(pageQuery.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //3.  转换实体 填充到vo
        List<Long> ids = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toList());
        //4. 远程调用课程服务 给vo三个字段赋值
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(ids);
        if(CollUtils.isEmpty(simpleInfoList)){
            throw new BizIllegalException("课程不存在");
        }
        //将得到的课程信息，变成map 方便在后面填充vo的时候使用
        Map<Long, CourseSimpleInfoDTO> map = simpleInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        List<LearningLessonVO> voList = new ArrayList<>();
        for (LearningLesson record : records) {
            LearningLessonVO learningLessonVO = BeanUtils.copyBean(record, LearningLessonVO.class);
            CourseSimpleInfoDTO simpleInfoDTO = map.get(record.getCourseId());

            if (simpleInfoDTO != null){
                learningLessonVO.setCourseName(simpleInfoDTO.getName());
                learningLessonVO.setCourseCoverUrl(simpleInfoDTO.getCoverUrl());
                learningLessonVO.setSections(simpleInfoDTO.getSectionNum());
                voList.add(learningLessonVO);
            }
        }
        //5. 返回vo
        return PageDTO.of(page, voList);
    }

    @Override
    public LearningLessonVO queryCurrentLesson() {
        //1. 找到当前的用户id
        Long userId = UserContext.getUser();
        //2. 在课程表里找到当前用户正在学习的课程 通过最近时间排序 找到最近的 作为正在学习的课程
        LearningLesson lesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1") //如果不加 直接one 当有多条数据时会抛出异常
                .one();
        if(lesson == null){
            return null;
        }
        //3. 调用课程微服务 找到课程信息 小节名称和编号
        LearningLessonVO learningLessonVO = BeanUtils.copyBean(lesson, LearningLessonVO.class);

        CourseFullInfoDTO course = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if(course == null){
            //说明数据不对啊 直接抛异常  如果数据为空或者其他正常情况 用return
            throw new BizIllegalException("课程不存在");
        }

        //4. 查询当前用户报名的课程总数
        Integer count = this.lambdaQuery().eq(LearningLesson::getUserId, userId).ne(LearningLesson::getStatus, LessonStatus.EXPIRED).count();
        //5. 远程调用小节服务
        Long latestSectionId = lesson.getLatestSectionId();
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(CollUtils.singletonList(latestSectionId));
        if(CollUtils.isEmpty(cataSimpleInfoDTOS)){
            throw new BizIllegalException("小节不存在");
        }
        learningLessonVO.setCourseName(course.getName());
        learningLessonVO.setCourseCoverUrl(course.getCoverUrl());
        learningLessonVO.setSections(course.getSectionNum());

        learningLessonVO.setCourseAmount(count);

        CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOS.get(0);
        learningLessonVO.setLatestSectionName(cataSimpleInfoDTO.getName());
        learningLessonVO.setLatestSectionIndex(cataSimpleInfoDTO.getCIndex());

        //5. 封装到vo 返回
        return learningLessonVO;
    }

    @Override
    public void removeUserLessons(Long userId, List<Long> courseIds) {

        this.lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds)
                .remove();
    }

    @Override
    public Long isLessonValid(Long courseId) {
        Long userId = UserContext.getUser();
         return this.lambdaQuery().eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId,userId)
                .ne(LearningLesson::getStatus, LessonStatus.EXPIRED)
                .last("limit 1")
                .oneOpt()
                .map(LearningLesson::getId)
                .orElse(null);
    }

    @Override
    public LearningLessonVO queryLessonStatus(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson learningLesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .last("limit 1")
                .one();
        if(learningLesson == null ){
            return null;
        }
        return BeanUtils.copyBean(learningLesson, LearningLessonVO.class);
        }

    @Override
    public Integer countNumByCourse(Long courseId) {
        if(courseId == null){
            throw new BizIllegalException("课程id不能为空");
        }
        return this.lambdaQuery().eq(LearningLesson::getCourseId, courseId)
                .count();
    }
}
