package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor //lombok提供的，使用构造器 在编译期生成相应的方法 构造注入
public class LessonChangeListener {

    final ILearningLessonService lessonService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void onMsg(OrderBasicDTO dto){
        log.info("LessonChangeListener接收到了消息：{}",dto);
        //1. 校验
        if(dto.getOrderId() == null || dto.getUserId() == null || CollUtils.isEmpty(dto.getCourseIds())){
            //如果校验不通过，不要抛异常。，直接return 不管就行。
            // 如果抛异常，会重试，重试次数达到上限，还是抛异常，就直接丢弃消息。
            return;
        }
        //2. 调用service,保存课程到课程表
        lessonService.addUserLessons(dto.getUserId(),dto.getCourseIds());
    }
}
