package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    @Override
    public void saveReply(ReplyDTO reply) {
        Long userId = UserContext.getUser();
        if(reply.getQuestionId() ==null){
            throw new BadRequestException("问题id不能为空");
        }
        if(StringUtils.isBlank(reply.getContent())){
            throw new BadRequestException("回复内容不能为空");
        }
        InteractionReply interactionReply = BeanUtils.copyBean(reply, InteractionReply.class);
        interactionReply.setUserId(userId);
        this.save(interactionReply);
    }

    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        if(query.getQuestionId()== null || query.getAnswerId() ==null){
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
