package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private static final List<String> BIZ_TYPES = List.of("QA", "NOTE");
    private static final int MAX_BIZ_SIZE = 30; //任务每次发送的数量，20个业务id

    private final ILikedRecordService recordService;

    @Scheduled(fixedDelay = 20000)
    public void checkLikedTimes(){
        for (String bizType : BIZ_TYPES) {
            //从redis中每次取30个业务id，分类别,防止一次发送mq 消息太多
            recordService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
        }
    }
}