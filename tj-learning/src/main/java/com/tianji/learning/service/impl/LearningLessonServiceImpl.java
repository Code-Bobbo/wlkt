package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbitmq.client.impl.PlainMechanism;
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
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    final LearningRecordMapper recordMapper;
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

    @Override
    public void createLearningPlan(Long courseId, Integer freq) {
        Long userId = UserContext.getUser();
        LearningLesson learningLesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .last("limit 1")
                .one();
        if(learningLesson == null){
            throw new BizIllegalException("用户未学习该课程");
        }
        //这种方式他会更新 所有的字段
        learningLesson.setWeekFreq(freq);
        learningLesson.setPlanStatus(PlanStatus.PLAN_RUNNING);
        this.updateById(learningLesson);
        //也可以使用链式编程 这种方式会更新set的字段
//        this.lambdaUpdate().eq(LearningLesson::getId,learningLesson.getId())
//                .set(LearningLesson::getWeekFreq,freq)
//                .update();
    }
 
    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {

        //1. 查询当前用户
        Long userId = UserContext.getUser();

        //2. 查询本周学习计划总数据
        QueryWrapper<LearningLesson> wrapper = new QueryWrapper<>();
        wrapper.select("sum(week_freq) as planTotal");//查询哪些列 也可以使用聚合函数，类似于miniob的select部分
        wrapper.eq("user_id", userId);
        wrapper.in("status", LessonStatus.NOT_BEGIN,LessonStatus.LEARNING);
        wrapper.eq("plan_status", PlanStatus.PLAN_RUNNING);
        Map<String, Object> map = this.getMap(wrapper);
        Integer i = 0;

        //防止出现空指针问题
        if(map != null && map.get("planTotal") != null){
            Object planTotal = map.get("planTotal");
        //得到的planTotal 是bigdecimal类型 需要类型转换  先转换为string 再转为int
             i = Integer.valueOf(planTotal.toString());
        }
        //3. 查询本周学习计划 已经学习的小节数据 条件是userid finish time在本周， finished为true
        //注入 record的mapper 避免循环依赖问题
        Integer weekFinishNum = recordMapper.selectCount(Wrappers.<LearningRecord>lambdaQuery().eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .between(LearningRecord::getFinishTime, DateUtils.getWeekBeginTime(LocalDate.now()), DateUtils.getWeekEndTime(LocalDate.now()))
        );

        //4. 查询课表信息 获取课程信息
        Page<LearningLesson> page = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .page(query.toMpPage("latest_learn_time", false));
        if(CollUtils.isEmpty(page.getRecords())){
            LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
            learningPlanPageVO.setTotal(0L);
            learningPlanPageVO.setPages(0L);
            learningPlanPageVO.setList(CollUtils.emptyList());
            return learningPlanPageVO;

        }
        Set<Long> courseIds = page.getRecords().stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(simpleInfoList)){
            throw new BizIllegalException("课程不存在");
        }
        Map<Long, CourseSimpleInfoDTO> courseSimpleInfoDTOMap = simpleInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        //5. 查询 本周 当前用户下的 每一门课下的以学习的小节数量 使用分组 并且为了方便使用  封装为map
//         QueryWrapper<LearningRecord> rWrapper = new QueryWrapper<>();
//        rWrapper.select("lesson_id","count(*) as userId");// 这里有个小技巧 将取到的count 结果 放到一个不相关的字段中，这样方便使用
//        rWrapper.eq("user_id", userId);
//        rWrapper.eq("finished",true);
//        rWrapper.between("finish_time", DateUtils.getWeekBeginTime(LocalDate.now()), DateUtils.getWeekEndTime(LocalDate.now()))
//        rWrapper.groupBy("lesson_id");
//        List<LearningRecord> learningRecords = recordMapper.selectList(rWrapper);
//        Map<Long, Long> courseweekFinishedNum = learningRecords.stream().collect(Collectors.toMap(LearningRecord::getLessonId, LearningRecord::getUserId));
            List<LearningRecord> learningRecords = recordMapper.selectList(Wrappers.<LearningRecord>query().eq("user_id", userId)
            .eq("finished", true)
            .between("finish_time", DateUtils.getWeekBeginTime(LocalDate.now()), DateUtils.getWeekEndTime(LocalDate.now()))
            .groupBy("lesson_id")
                    .select("lesson_id","count(*) as userId"));
            Map<Long, Long> courseweekFinishedNum = learningRecords.stream().collect(Collectors.toMap(LearningRecord::getLessonId, LearningRecord::getUserId));
        //6。 封装vo 返回
        LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
        learningPlanPageVO.setWeekTotalPlan(i);
        learningPlanPageVO.setWeekFinished(weekFinishNum);
        ArrayList<LearningPlanVO> learningPlanVOS = new ArrayList<>();
        for (LearningLesson record : page.getRecords()) {
            LearningPlanVO planVO = BeanUtils.copyBean(record, LearningPlanVO.class);
            CourseSimpleInfoDTO simpleInfoDTO = courseSimpleInfoDTOMap.get(record.getCourseId());
            if(simpleInfoDTO != null){
                planVO.setCourseName(simpleInfoDTO.getName());
                planVO.setSections(simpleInfoDTO.getSectionNum());
            }
            //本周以学习的小节数量
            Long num = courseweekFinishedNum.get(record.getCourseId());
            if(num ==null){
                planVO.setWeekLearnedSections(0);
            }else{
                planVO.setWeekLearnedSections(num.intValue());
            }

            learningPlanVOS.add(planVO);
        }
        learningPlanPageVO.setList(learningPlanVOS);
        learningPlanPageVO.setTotal(page.getTotal());
        learningPlanPageVO.setPages(page.getPages());
        return learningPlanPageVO;
    }
}
