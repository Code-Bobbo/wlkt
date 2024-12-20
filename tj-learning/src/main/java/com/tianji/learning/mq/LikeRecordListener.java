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
    public void onMsg(LikedTimesDTO dto){
        log.info("LikeRecordListener接收到了消息：{}",dto);
        //1. 校验
        if(dto.getLikedTimes() == null || dto.getBizId() == null){
            //如果校验不通过，不要抛异常。，直接return 不管就行。
            // 如果抛异常，会重试，重试次数达到上限，还是抛异常，就直接丢弃消息。
            return;
        }
        //2. 调用service
        InteractionReply interactionReply = replyService.getById(dto.getBizId());
        if (interactionReply == null)
            return;
        interactionReply.setLikedTimes(dto.getLikedTimes());
        replyService.updateById(interactionReply);
    }

}
