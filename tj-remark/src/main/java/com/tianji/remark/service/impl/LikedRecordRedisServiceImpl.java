package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.dto.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
        private final RabbitMqHelper rabbitMqHelper;
        private final StringRedisTemplate stringRedisTemplate;
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
        //使用redis统计该业务id的总点赞数
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + form.getBizId();
        Long totalLikesNums = stringRedisTemplate.opsForSet().size(key);
        if (totalLikesNums == null){
            return;
        }
        //缓存点赞的总数
        String bizTypeKey = RedisConstants.LIKE_COUNT_KEY_PREFIX + form.getBizType();
        stringRedisTemplate.opsForZSet().add(bizTypeKey,form.getBizId().toString(),totalLikesNums);
//        //发送消息到mq
//        String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, form.getBizType());
//        LikedTimesDTO likedTimesDTO = new LikedTimesDTO();
//        likedTimesDTO.setLikedTimes(totalLikesNums);
//        likedTimesDTO.setBizId(form.getBizId());
//        rabbitMqHelper.send(
//                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
////                form.getBizType().equals("QA.times.changed")? MqConstants.Key.QA_LIKED_TIMES_KEY :MqConstants.Key.NOTE_LIKED_TIMES_KEY,\
//                routingKey,
//                likedTimesDTO
//        );


    }

    @Override
    public Set<Long> getLikedStatusByBizIds(List<Long> bizIds) {


        // 1.获取登录用户id
        Long userId = UserContext.getUser();
        // 2.查询点赞状态 短时间执行大量的redis命令 使用管道技术
        List<Object> objects = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
                src.sIsMember(key, userId.toString());
            }
            return null;
        });
        // 3.返回结果
        return IntStream.range(0, objects.size()) // 创建从0到集合size的流
                .filter(i -> (boolean) objects.get(i)) // 遍历每个元素，保留结果为true的角标i
                .mapToObj(bizIds::get)// 用角标i取bizIds中的对应数据，就是点赞过的id
                .collect(Collectors.toSet());// 收集


    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        String key = RedisConstants.LIKE_COUNT_KEY_PREFIX + bizType;
        //从redis中去获取并移除数据,按照分数排序取maxBizSize的业务点赞信息  最小数量的
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().popMin(key, maxBizSize);
        //封装数据到dto 方便mq发送
        List<LikedTimesDTO> dtoList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String bizid = typedTuple.getValue();
            Double likeTimes = typedTuple.getScore();
            if (StringUtils.isBlank(bizid) || likeTimes == null){
                continue;
            }
            LikedTimesDTO likedTimesDTO= new LikedTimesDTO();
            likedTimesDTO.setBizId(Long.valueOf(bizid));
            likedTimesDTO.setLikedTimes(likeTimes.intValue());
            dtoList.add(likedTimesDTO);

        }
        if (CollUtils.isEmpty(dtoList)){
            return;
        }
        //批量发送消息到mq
        String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType);
        rabbitMqHelper.send(
                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                routingKey,
                dtoList
        );
    }

    private boolean unlike(LikeRecordFormDTO form, Long userId) {
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + form.getBizId();
        Long removed = stringRedisTemplate.opsForSet().remove(key, userId.toString());//如果移除成功返回值1
        return removed != null && removed > 0;
    }

    private boolean liked(LikeRecordFormDTO form, Long userId) {
      //基于redis 做点赞
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + form.getBizId();
        Long flag = stringRedisTemplate.opsForSet().add(key, userId.toString()); // 返回值是成功插入了多少行
        //由于set 不允许重复，因此插入重复的value 返回值会是0
        return flag != null && flag > 0;
    }
}
