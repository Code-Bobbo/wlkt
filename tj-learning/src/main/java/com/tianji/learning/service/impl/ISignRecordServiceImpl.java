package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ISignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitMqHelper rabbitMqHelper;
    @Override
    public SignResultVO sign() {
        // 获取用户id
        Long userId = UserContext.getUser();
        //固定redis 前缀key 拼接
        LocalDate now = LocalDate.now();//当前时间的年月
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM")); //得到冒号年月 的格式字符串
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX+userId.toString() +format;
        int offset = now.getDayOfMonth() - 1;
        //使用redis bitmap 保存到redis中 1表示签到，0表示未签到 需要校验签到状态
        Boolean setBit = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        if (setBit){
            throw new BizIllegalException("已经签到过了");
        }

        //计算每个月连续签到的天数
            //得到本月到现在的签到数据
        int signDays = countSignDays(key, now.getDayOfMonth());

        //计算连续签到的奖励积分
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        //保存积分 发送给消息到mq
        rabbitMqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1));// 签到积分是基本得分+奖励积分
        //封装vo 返回
        SignResultVO signResultVO = new SignResultVO();
        signResultVO.setSignDays(signDays);
        signResultVO.setSignPoints(rewardPoints);
        return  signResultVO;
    }

    @Override
    public Byte[] querySignResult() {
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM")); //得到冒号年月 的格式字符串
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX+userId.toString() +format;
        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(now.getDayOfMonth())).valueAt(0));
        if(CollUtils.isEmpty(result)){
            return new Byte[0];
        }
        Long num = result.get(0);
        //声明的偏移量 相当于是数组下标 从0开始
        int offset = now.getDayOfMonth() - 1;
        //利用与运算和位移封装结构
        Byte[] b = new Byte[now.getDayOfMonth()];
        while (offset >=0) { //与1 进行与运算 得到最后bit 的值 与1比较 表示判断是否为1
            //对每一位进行赋值 根据索引 从后往前
            b[offset] = (byte) (num & 1);
            offset--;
            //右移
            num >>>= 1;
        }

        return b;
    }

    private int countSignDays(String key, int len) {
        // 1.获取本月从第一天开始，到今天为止的所有签到记录
        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(len)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return 0;
        }
        int num = result.get(0).intValue(); //得到的是十进制，但是没关系 进行与运算的时候会自动转为二进制
        log.info("num：{}", num);
        //计算有多少天连续签到
        // 2.定义一个计数器
        int count = 0;
        // 3.循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1) { //与1 进行与运算 得到最后bit 的值 与1比较 表示判断是否为1
            // 4.计数器+1
            count++;
            // 5.把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            //进行无符号右移动
            num >>>= 1;
        }
        return count;
    }
}
