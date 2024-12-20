package com.tianji.learning.mq;

import com.tianji.api.dto.message.LikedTimesDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.QA_LIKED_TIMES_KEY;

@Component
@Slf4j
@RequiredArgsConstructor //lombok提供的，使用构造器 在编译期生成相应的方法 构造注入
public class LikeRecordListener {

    final IInteractionReplyService replyService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "qa.liked.times.queue", durable = "true"),
            exchange = @Exchange(name = LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = QA_LIKED_TIMES_KEY
    ))
    public void onMsg(List<LikedTimesDTO> dtoList){
        log.info("LikeRecordListener接收到了消息：{}",dtoList);
        List<InteractionReply> interactionReplyList =new ArrayList<>();
        for (LikedTimesDTO likedTimesDTO : dtoList) {
            InteractionReply interactionReply = new InteractionReply();
            interactionReply.setId(likedTimesDTO.getBizId());
            interactionReply.setLikedTimes(likedTimesDTO.getLikedTimes());
            interactionReplyList.add(interactionReply);
        }
        replyService.updateBatchById(interactionReplyList);
    }

}
