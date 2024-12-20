package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author bob
 * @since 2024-12-04
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {
    private final InteractionQuestionMapper questionMapper;
    @Override
    public void saveReply(ReplyDTO reply) {
        Long userId = UserContext.getUser();
        InteractionReply interactionReply = BeanUtils.copyBean(reply, InteractionReply.class);
        interactionReply.setUserId(userId);
        this.save(interactionReply);
        InteractionQuestion question = questionMapper.selectById(reply.getQuestionId());;

        if(reply.getAnswerId() != null){
            //提交的评论，要累计回答的评论数量,这种更新方式 会访问两次数据库，使用lamddaUpdate 更新只会调用一次
            InteractionReply byId = this.getById(reply.getAnswerId());
            byId.setReplyTimes(byId.getReplyTimes() + 1);
            this.updateById(byId);
        }else{
            //提交的回答 要累计互动问题的回答数量

            question.setAnswerTimes(question.getAnswerTimes() + 1);
            question.setLatestAnswerId(interactionReply.getId());

        }
        if(reply.getIsStudent()){
            question.setStatus(QuestionStatus.UN_CHECK);
        }
        questionMapper.updateById(question);

    }

    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        if(query.getQuestionId()== null && query.getAnswerId() ==null){
            throw new BadRequestException("参数错误");
        }
        Page<InteractionReply> page = this.lambdaQuery().eq(query.getQuestionId() != null, InteractionReply::getQuestionId, query.getQuestionId())
                .eq(query.getAnswerId() !=null, InteractionReply::getAnswerId, query.getAnswerId())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionReply> records = page.getRecords();
        List<ReplyVO> replyVOS = BeanUtils.copyList(records, ReplyVO.class);

        return PageDTO.of(page, replyVOS);
    }
}
