package com.tianji.remark.service.impl;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.dto.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author bob
 * @since 2024-12-18
 */
@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
        private final RabbitMqHelper rabbitMqHelper;
    @Override
    public void likeOrCancel(LikeRecordFormDTO form) {
        Long userId = UserContext.getUser();

        Boolean like = form.getLiked();

        boolean flag = false;
        if(form.getLiked()){
        //点赞逻辑
           flag = liked(form,userId);

        }else{
            flag = unlike(form,userId);
        }

        if(!flag){
            //说明点赞或者取消赞失败
            return;
        }
        //统计该业务id的总点赞数
        Integer totalLikesNums = this.lambdaQuery()
                .eq(LikedRecord::getBizId, form.getBizId())
                .count();
        //发送消息到mq
        String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, form.getBizType());
        LikedTimesDTO likedTimesDTO = new LikedTimesDTO();
        likedTimesDTO.setLikedTimes(totalLikesNums);
        likedTimesDTO.setBizId(form.getBizId());
        rabbitMqHelper.send(
                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
//                form.getBizType().equals("QA.times.changed")? MqConstants.Key.QA_LIKED_TIMES_KEY :MqConstants.Key.NOTE_LIKED_TIMES_KEY,\
                routingKey,
                likedTimesDTO
        );


    }

    @Override
    public Set<Long> getLikedStatusByBizIds(List<Long> bizIds) {

        Long userId= UserContext.getUser();
        return this.lambdaQuery().eq(LikedRecord::getUserId, userId)
                .in(LikedRecord::getBizId, bizIds)
                .select(LikedRecord::getBizId)
                .list()
                .stream()
                .map(LikedRecord::getBizId)
                .collect(Collectors.toSet());

    }

    private boolean unlike(LikeRecordFormDTO form, Long userId) {

        LikedRecord record = this.lambdaQuery().eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, form.getBizId())
                .one();
        if(record == null){
            //说明之前没点过赞
            return false;
        }
        return this.removeById(record.getId());
    }

    private boolean liked(LikeRecordFormDTO form, Long userId) {
        LikedRecord record = this.lambdaQuery().eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, form.getBizId())
                .one();
        if(record != null){
            //说明之前点过赞
            return false;
        }
        LikedRecord likedRecord = BeanUtils.copyBean(form, LikedRecord.class);
        likedRecord.setUserId(userId);
        return this.save(likedRecord);
    }
}
