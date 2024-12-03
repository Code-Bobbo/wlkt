package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author bob
 * @since 2024-11-13
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    final ILearningLessonService lessonService;
    final CourseClient courseClient;
    final LearningRecordDelayTaskHandler taskHandler;
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        //1 获取当前用户
        Long userId = UserContext.getUser();

        // 2 查询课程信息 拿到lesson id
        LearningLesson learningLesson = lessonService.lambdaQuery().eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, userId)
                .one();
        if(learningLesson == null){
            throw new BizIllegalException("用户未学习该课程");
        }

        //3. 查询学习记录
        List<LearningRecord> learningRecords = this.lambdaQuery().eq(LearningRecord::getLessonId, learningLesson.getId())
                .eq(LearningRecord::getUserId, userId)
                .list();

        //4. 封装结果返回
        LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
        learningLessonDTO.setId(learningLesson.getId());
        learningLessonDTO.setLatestSectionId(learningLesson.getLatestSectionId());
        List<LearningRecordDTO> learningRecordDTOS = BeanUtils.copyList(learningRecords, LearningRecordDTO.class);
        learningLessonDTO.setRecords(learningRecordDTOS);
        return learningLessonDTO;

    }

    @Override
    public void addLearningRecord(LearningRecordFormDTO dto) {
//        //1 获取登录用户
//        Long userId = UserContext.getUser();
//        //2 判断提交的是否是考试类型
//
//        //如果是考试类型，直接新增学习记录，并且完成小节数加1 完善lesson表的的三个字段
//        if(dto.getSectionType() == SectionType.EXAM){
//            LearningRecord learningRecord = new LearningRecord();
//            learningRecord.setLessonId(dto.getLessonId());
//            learningRecord.setSectionId(dto.getSectionId());
//            learningRecord.setMoment(dto.getMoment());
//            learningRecord.setUserId(userId);
//            learningRecord.setFinishTime(dto.getCommitTime());
//            learningRecord.setCreateTime(dto.getCommitTime());
//            learningRecord.setFinished(true);
//            boolean saveExamRecord = this.save(learningRecord);
//
//            if(!saveExamRecord){
//                throw new BizIllegalException("添加考试学习记录失败");
//            }
//            //并且完成小节数加1 完善lesson表的的三个字段
//            boolean updatelesson = lessonService.lambdaUpdate().eq(LearningLesson::getId, dto.getLessonId())
//                    .set(LearningLesson::getLatestLearnTime, dto.getCommitTime())
//                    .set(LearningLesson::getLatestSectionId, dto.getSectionId())
//                    .set(LearningLesson::getPlanStatus , LessonStatus.LEARNING)
//                    .setSql(saveExamRecord, "learned_sections = learned_sections + 1")
//                    .set(LearningLesson::getUpdateTime, dto.getCommitTime())
//                    .update();
//            if(!updatelesson){
//                throw new BizIllegalException("更新课程信息失败");
//            }
//
//        }else if(dto.getSectionType() == SectionType.VIDEO){
//            //如果是视频类型，判断是否是第一次提交，如果是，则更新学习记录，并且当时间大于时长的一半的时候完成小节数加1， 完善lesson表的三个字段，否则更新学习记录
//            LearningRecord learningRecord = this.lambdaQuery().eq(LearningRecord::getUserId, userId)
//                    .eq(LearningRecord::getLessonId, dto.getLessonId())
//                    .eq(LearningRecord::getSectionId, dto.getSectionId())
//                    .last("limit 1")
//                    .one();
//            LearningLesson learningLesson = lessonService.lambdaQuery()
//                    .eq(LearningLesson::getId, dto.getLessonId())
//                    .one();
//
//            if(learningRecord == null){
//                //说明是第一次提交 需要创建记录
//                LearningRecord record = new LearningRecord();
//                record.setLessonId(dto.getLessonId());
//                record.setUserId(userId);
//                record.setSectionId(dto.getSectionId());
//                record.setMoment(dto.getMoment());
//                record.setCreateTime(dto.getCommitTime());
//                record.setUpdateTime(dto.getCommitTime());
//                boolean saveFirstVedio = this.save(record);
//                if(!saveFirstVedio){
//                    throw new BizIllegalException("添加视频学习记录失败");
//                }
//                boolean b = lessonService.lambdaUpdate().eq(LearningLesson::getId, dto.getLessonId())
//                        .set(LearningLesson::getStatus, LessonStatus.LEARNING)
//                        .set(LearningLesson::getLatestLearnTime, dto.getCommitTime())
//                        .set(LearningLesson::getLatestSectionId, dto.getSectionId())
//                        .set(LearningLesson::getUpdateTime, dto.getCommitTime())
//                        .update();
//                if(!b){
//                    throw new BizIllegalException("更新第一次视频类型课程信息失败");
//                }
//
//            }else{
//                //不是第一次提交视频类型 需要更新record 还要根据小节数 更新lesson
//                boolean finished = dto.getDuration() / dto.getMoment() <= 2; //这一小节是否完成
//                learningRecord.setMoment(dto.getMoment()).setUpdateTime(dto.getCommitTime());
//                if(finished){
//                    learningRecord.setFinished(true).setFinishTime(dto.getCommitTime());
//                }
//                boolean update1 = this.lambdaUpdate().update(learningRecord);
//                if(!update1){
//                    throw new BizIllegalException("更新视频学习记录失败");
//                }
//
//                //如果这一小节 看完了 更新lesson表的相关字段
//                if(finished){
//                    //上面的小节完成 lesson表的完成小节数加1 完善lesson表的最新学习时间
//                    //还需要判断是否所有的小节 都看完了 ，然后需要更新课程的学习状态
//
//                    CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
//                    if (cInfo == null) {
//                        throw new BizIllegalException("课程不存在，无法更新数据！");
//                    }
//                     boolean allLearned = learningLesson.getLearnedSections() + 1 >= cInfo.getSectionNum();
//                    boolean update = lessonService.lambdaUpdate().
//                            eq(LearningLesson::getId, dto.getLessonId())
//                            .set(LearningLesson::getLatestLearnTime, dto.getCommitTime())
//                            .set(LearningLesson::getLatestSectionId, dto.getSectionId())
//                            .set(LearningLesson::getUpdateTime, dto.getCommitTime())
//                            .set(allLearned,LearningLesson::getPlanStatus , LessonStatus.FINISHED)
//                            .setSql("learned_sections = learned_sections + 1")
//                            .update();
//                    if(!update){
//                        throw new BizIllegalException("更新课程信息失败");
//                    }
//                }
//
//            }
//        }
//
//
//        //如果是视频类型，判断是否是第一次提交，如果是，则更新学习记录，并且当时间大于时长的一半的时候完成小节数加1， 完善lesson表的三个字段，否则更新学习记录
//    }
        //重新写
        Long userId = UserContext.getUser();
        boolean isFinished = false;
        if(dto.getSectionType().equals(SectionType.VIDEO)){
            //执行视频操作
            isFinished = handleVideoRecord(userId, dto);
        } else{
            //执行考试操作
            isFinished = handleExamRecord(userId, dto);
        }
        //如果不是第一次学完，直接return 终止方法执行，不走下面处理课程数据调用db的方法
        if(!isFinished){
            return;
        }

        //处理课表数据
        handleLessonData(dto, isFinished);
    }

    private void handleLessonData(LearningRecordFormDTO dto, boolean isFinished) {
        // 1.查询课表
        LearningLesson lesson = lessonService.getById(dto.getLessonId());
        if (lesson == null) {
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        // 2.判断是否有新的完成小节
        boolean allLearned = false;
        if(isFinished){
            // 3.如果有新完成的小节，则需要查询课程数据
            CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
            if (cInfo == null) {
                throw new BizIllegalException("课程不存在，无法更新数据！");
            }
            // 4.比较课程是否全部学完：已学习小节 >= 课程总小节
            allLearned = lesson.getLearnedSections() + 1 >= cInfo.getSectionNum();
        }
        // 5.更新课表
        lessonService.lambdaUpdate()
                .set(lesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING)
                .set(allLearned, LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
                .set(!isFinished, LearningLesson::getLatestSectionId, dto.getSectionId())
                .set(!isFinished, LearningLesson::getLatestLearnTime, dto.getCommitTime())
                //在这里如果要先查出旧数据 然后+1的话，否则会因为并发问题导致数据错误 两个进程进来获取的旧数据相同 然后都+1 导致数据错误 少加一个
                //但是在这个场景下 并发量不高，所以不用考虑这个问题 先查出数据 然后加一也行
                .setSql(isFinished, "learned_sections = learned_sections + 1")
                .eq(LearningLesson::getId, lesson.getId())
                .update();
    }

    private boolean handleExamRecord(Long userId, LearningRecordFormDTO dto) {
        // 1.转换DTO为PO
        LearningRecord record = BeanUtils.copyBean(dto, LearningRecord.class);
        // 2.填充数据
        record.setUserId(userId);
        record.setFinished(true);
        //只需要设置完成时间，创建时间和更新时间 在数据库都有默认当前时间戳
        record.setFinishTime(dto.getCommitTime());
        // 3.写入数据库
        boolean success = this.save(record);
        if (!success) {
            throw new DbException("新增考试记录失败！");
        }
        return true;
    }

    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO dto) {
        // 1.查询旧的学习记录
        LearningRecord old = queryOldRecord(dto.getLessonId(), dto.getSectionId());
        // 2.判断是否存在
        if (old == null) {
            // 3.不存在，则新增
            // 3.1.转换PO
            LearningRecord record = BeanUtils.copyBean(dto, LearningRecord.class);
            // 3.2.填充数据
            record.setUserId(userId);
            // 3.3.写入数据库
            boolean success = save(record);
            if (!success) {
                throw new DbException("新增学习记录失败！");
            }
            return false;  //否则还会往下走
        }
        // 4.存在，则更新
        // 4.1.判断是否是第一次完成
        boolean finished = !old.getFinished() && dto.getMoment() * 2 >= dto.getDuration();
        if(!finished){
            LearningRecord record = new LearningRecord();
            record.setId(old.getId()).setFinished(old.getFinished()).setMoment(dto.getMoment()).setLessonId(dto.getLessonId()).setSectionId(dto.getSectionId());
            taskHandler.addLearningRecordTask(record);
            return false; //代表本小节没有学完
        }
        // 4.2.更新数据
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, dto.getMoment())
                .set(finished, LearningRecord::getFinished, true)
                .set(finished, LearningRecord::getFinishTime, dto.getCommitTime())
                .eq(LearningRecord::getId, old.getId())
                .update();
        if (!success) {
            throw new DbException("更新学习记录失败！");
        }
        taskHandler.cleanRecordCache(dto.getLessonId(), dto.getSectionId());
        return finished;
    }

    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {
        //1. 查询缓存 如果命中 直接返回，如果未命中 查询db
        LearningRecord record = taskHandler.readRecordCache(lessonId, sectionId);
        if (record != null) {
            return record;
        }
        //2. 查询db
        record =    lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        if(record ==null){
            return null; //走被调用方法的 新建学习记录的分支
        }
        //放入缓存 如果上面不判断段是否为null的话，会导致下面的方法空指针异常
        taskHandler.writeRecordCache(record);

        return record;

    }

}
