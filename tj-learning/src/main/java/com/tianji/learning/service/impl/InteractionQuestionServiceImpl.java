package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageAdminQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author bob
 * @since 2024-12-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final UserClient userClient;
    private final IInteractionReplyService replyService;
    private final SearchClient searchClient;

    @Override
    public void save(QuestionFormDTO dto) {
        //1. 获取当前用户
        Long userId = UserContext.getUser();
        //2. dto转po
        InteractionQuestion question = BeanUtils.copyBean(dto, InteractionQuestion.class);
        question.setUserId(userId);
        this.save(question);
    }

    @Override
    public void updateQuestion(Long id, QuestionFormDTO dto) {
        //1.校验
        if(StringUtils.isBlank(dto.getTitle()) || StringUtils.isBlank(dto.getDescription()) || dto.getAnonymity() ==null){
            throw new BadRequestException("非法参数");
        }

        //校验id
        InteractionQuestion question = this.getById(id);
        if(question.getId() == null){
            throw new BadRequestException("非法参数");
        }
        //只能修改自己的问题
        if(!UserContext.getUser().equals(question.getUserId())){
            throw new BadRequestException("只能修改本人问题");
        }
        //dto转po
        question.setTitle(dto.getTitle());
        question.setDescription(dto.getDescription());
        question.setAnonymity(dto.getAnonymity());
        this.updateById(question);

    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        if(query.getCourseId() ==null){
            throw new BadRequestException("课程id不能为空");
        }
        Long userId = UserContext.getUser();
        Page<InteractionQuestion> page = this.lambdaQuery()
                //在查询的时候，不查询description字段
                .select(InteractionQuestion.class, new Predicate<TableFieldInfo>() {
                    @Override
                    public boolean test(TableFieldInfo tableFieldInfo) {

                        return ! tableFieldInfo.getProperty().equals("description");
                    }
                })
                .eq(InteractionQuestion::getCourseId, query.getCourseId())
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, userId)
                .eq(query.getSectionId() != null, InteractionQuestion::getSectionId, query.getSectionId())
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        //得到该课程的问题列表
        List<InteractionQuestion> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //根据最新回答 批量查询回答信息
        //得到最新回答列表的id
        Set<Long> latestAnswerIds = records.stream()
                .filter(c ->c.getLatestAnswerId() !=null)
                .map(InteractionQuestion::getLatestAnswerId)
                .collect(Collectors.toSet());
        //互动问题的用户id集合
        Set<Long> userIds = records.stream()
                .filter(c -> !c.getAnonymity())
                .map(InteractionQuestion::getUserId)
                .collect(Collectors.toSet());


        //远程调用 回答服务，找到最新回答 的记录，并且转为map 方便使用
        Map<Long, InteractionReply> replyMap = new HashMap<>();
        if (CollUtils.isNotEmpty(latestAnswerIds)) {
//            List<InteractionReply> replyList = replyService.listByIds(latestAnswerIds);
            List<InteractionReply> replyList = replyService.lambdaQuery()
                    .in(InteractionReply::getId, latestAnswerIds)
                    .eq(InteractionReply::getHidden, false)
                    .list();
            //变形为map 方便放到vo中
            replyMap = replyList.stream()
                    .collect(Collectors.toMap(InteractionReply::getId, r -> r));
            //将回答问题的用户id 也存入 userId中
            userIds = replyList.stream()
                    .filter(r -> ! r.getAnonymity())
                    .map(InteractionReply::getUserId)
                    .collect(Collectors.toSet());

        }

        //远程调用用户服务 批量获取用户信息

        List<UserDTO> userDTOList = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = userDTOList.stream()
                .collect(Collectors.toMap(UserDTO::getId, u -> u));

        //封装vo 返回
        List<QuestionVO> voList = new ArrayList<>();
        for (InteractionQuestion record : records) {
            //因为在for循环中要把最新回答 和用户信息 封装到每一个vo中，随所以使用copybean方法 将po挨个copy之后 然后set;要是没补充的字段 直接copyList
            QuestionVO questionVO = BeanUtils.copyBean(record, QuestionVO.class);
            //如果用户是匿名
            if (! questionVO.getAnonymity()) {
                UserDTO userDTO = userMap.get(record.getUserId());
                if (userDTO != null) {
                    questionVO.setUserName(userDTO.getName());
                    questionVO.setUserIcon(userDTO.getIcon());
                }
            }
            InteractionReply interactionReply = replyMap.get(record.getLatestAnswerId());
            if (interactionReply != null) {
                questionVO.setLatestReplyContent(interactionReply.getContent());
                if (! interactionReply.getAnonymity()){
                    UserDTO replyDTO = userMap.get(interactionReply.getUserId());
                    if (replyDTO != null) {
                        questionVO.setLatestReplyUser(replyDTO.getName());
                    }
                }
            }
            voList.add(questionVO);
        }
        return PageDTO.of(page, voList);
    }

    @Override
    public QuestionVO getQuestionById(Long id) {
        //1. 参数校验
        if (id == null){
            throw new BadRequestException("id不能为null");
        }
        InteractionQuestion question = this.getById(id);
        if (question == null){
            return null;
        }
        if (question.getHidden()){
            return null;
        }
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (! question.getAnonymity()){
            UserDTO userDTO = userClient.queryUserById(question.getUserId());
            if (userDTO != null) {
                vo.setUserName(userDTO.getName());
                vo.setUserIcon(userDTO.getIcon());
            }
        }
        return vo;

    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionAdminPage(QuestionAdminPageQuery query) {
        //参数中有课程名称，而数据库表中没有，如果传了，需要从es中获取名称对应的课程id，在search微服务中进行查询，本质上es就是在一个方法内部，创建链接 查询 返回结果集合
        String courseName = query.getCourseName();
        List<Long> courseIds = null;
        if(StringUtils.isNotEmpty(courseName)){
            //通过figin 远程调用搜索服务，从es中搜索关键字课程名称，返回课程id
            List<Long> courseIds = searchClient.queryCoursesIdByName(courseName);
            //如果在es 中查询不到 则返回空
            if (CollUtils.isEmpty(courseIds)){
            return PageDTO.empty(0L,0L);}

        }
        Page<InteractionQuestion> questionPage = this.lambdaQuery()
                .in(InteractionQuestion::getCourseId, courseIds)
                .eq(query.getStatus(), InteractionQuestion::getStatus, query.getStatus())
                .between(query.getBeginTime() != null && query.getEndTime() != null,
                        InteractionQuestion::getCreateTime, query.getBeginTime(), query.getEndTime())

                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = questionPage.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(0L,0L);
        }
    }

}
