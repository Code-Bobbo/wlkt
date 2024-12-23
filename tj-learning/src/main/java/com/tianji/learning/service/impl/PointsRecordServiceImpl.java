package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author bob
 * @since 2024-12-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate stringRedisTemplate;


    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        LocalDateTime now = LocalDateTime.now();
        int maxPoints = type.getMaxPoints();
        // 1.判断当前方式有没有积分上限
        int realPoints = points; //实际可以增加的积分
        if(maxPoints > 0) {
            // 2.有，则需要判断是否超过上限
            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            // 2.1.查询今日已得积分
            int currentPoints = queryUserPointsByTypeAndDate(userId, type, begin, end);
            // 2.2.判断是否超过上限
            if(currentPoints >= maxPoints) {
                // 2.3.超过，直接结束
                return;
            }
            // 2.4.没超过，保存积分记录
            //如果 当前积分加上本次积分大于上限，则本次积分为上限减去当前积分
            if(currentPoints + points > maxPoints){
                realPoints = maxPoints - currentPoints;
            }
        }
        // 3.没有，直接保存积分记录
        PointsRecord p = new PointsRecord();
        p.setPoints(realPoints);
        p.setUserId(userId);
        p.setType(type);
        save(p);
        //累加并保存总积分值到redis 采用zset结构 调用累加api 生成实时排行榜
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        stringRedisTemplate.opsForZSet().incrementScore(key, userId.toString(), realPoints);
    }

    @Override
    public List<PointsStatisticsVO> queryTodayPoints() {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper
                .eq("user_id", userId)
                .between(begin != null && end != null, "create_time", begin, end)
                .groupBy("type" )
                .select("type,sum(points) as points");
        List<PointsRecord> list = this.list(wrapper);
        if (CollUtils.isEmpty(list)){
            return CollUtils.emptyList();
        }
        log.info("type list：{}", list);
        List<PointsStatisticsVO> pointsStatisticsVOS = new ArrayList<>();
        for (PointsRecord pointsRecord : list) {
            PointsStatisticsVO pointsStatisticsVO = new PointsStatisticsVO();
            pointsStatisticsVO.setPoints(pointsRecord.getPoints());
            pointsStatisticsVO.setType(pointsRecord.getType().getDesc());
            pointsStatisticsVO.setMaxPoints(pointsRecord.getType().getMaxPoints());
            pointsStatisticsVOS.add(pointsStatisticsVO);
        }
        return pointsStatisticsVOS;

    }

    private int queryUserPointsByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        // 1.查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper
                .eq("user_id", userId)
                .eq(type != null, "type", type)
                .between(begin != null && end != null, "create_time", begin, end)
                .select("sum(points) as totalPoints");
        Map<String, Object> map = this.getMap(wrapper);
        int intValue = 0;
        if(map != null){
            BigDecimal totalPoints = (BigDecimal) map.get("totalPoints");
            intValue = totalPoints.intValue();

        }
        // 2.调用mapper，查询结果
        //使用getBaseMapper 会得到dao层的对象，然后调用dao层的queryUserPointsByTypeAndDate方法，该方法是mybatis的方法
        Integer points = getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
        // 3.判断并返回
        return intValue;
    }
}
