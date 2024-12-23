package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author bob
 * @since 2024-12-21
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate stringRedisTemplate;
    private final UserClient userClient;
    @Override
    public PointsBoardVO queryPointsBoard(PointsBoardQuery query) {
        //判断查当前赛季 还是历史赛季
        LocalDateTime now = LocalDateTime.now();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        boolean isNow = query.getSeason() == null || query.getSeason() == 0;
        Long season = query.getSeason(); //历史赛季id
        //进行查询我的排名和总积分  抽取出方法
        //分页查询赛季列表 根据season 判断查询redis还是db
        PointsBoard pointsBoard =null;
        List<PointsBoard> list = new ArrayList<>();
        if(isNow){
            //查询当前赛季我的排名和积分
            pointsBoard = queryPointsBoardNow(key);
            list = queryPointsBoardNowList(key,query.getPageNo(),query.getPageSize());
        }else{
            //查询历史赛季我的排名和积分
            pointsBoard = queryPointsBoardHistory(season);
            list = queryPointsBoardHistoryList();
        }
        PointsBoardVO vo = new PointsBoardVO();
        if (pointsBoard !=null ) {
            vo.setRank(pointsBoard.getRank());
            vo.setPoints(pointsBoard.getPoints());
        }
        // 4.2.查询用户信息
        Set<Long> uIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = new HashMap<>(uIds.size());
        if(CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        List<PointsBoardItemVO> voList = new ArrayList<>();
        for (PointsBoard board : list) {
            PointsBoardItemVO pointsBoardItemVO = new PointsBoardItemVO();
            pointsBoardItemVO.setPoints(board.getPoints());
            pointsBoardItemVO.setRank(board.getRank());
            pointsBoardItemVO.setName(userMap.get(board.getUserId()));

            voList.add(pointsBoardItemVO);
        }
        vo.setBoardList(voList);

        return vo;
    }

    @Override
    public void createPointsBoardTableBySeason(Integer id) {

        getBaseMapper().createPointsBoardTable(LearningConstants.POINTS_BOARD_TABLE_PREFIX + id);
    }

    private List<PointsBoard> queryPointsBoardHistoryList() {
        return null;
    }

    private PointsBoard queryPointsBoardHistory(Long season) {
        return null;
    }

    //查询当前赛季排行榜信息列表
    private List<PointsBoard> queryPointsBoardNowList(String key, @Min(value = 1, message = "页码不能小于1") Integer pageNo, @Min(value = 1, message = "每页查询数量不能小于1") Integer pageSize) {
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, (pageNo - 1) * pageSize, pageNo * pageSize - 1);
        if(CollUtils.isEmpty(typedTuples)){
            return CollUtils.emptyList();
        }
        List<PointsBoard> list = new ArrayList<>();
        //每次分页查询的排名要与分页查询的起始位置做加一操作
        int from = (pageNo - 1) * pageSize;
        int i = from + 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String value = typedTuple.getValue();//用户id
            Double score = typedTuple.getScore();//用户得分
            if(StringUtils.isBlank(value) || score == null){
                continue; //结束本次循环，继续下一次循环
            }
            PointsBoard pointsBoard = new PointsBoard();
            pointsBoard.setUserId(Long.valueOf(value));
            pointsBoard.setPoints(score.intValue());
            pointsBoard.setRank(i++);
            list.add(pointsBoard);
        }
        return list;
    }

    private PointsBoard queryPointsBoardNow(String key) {

        Long userId = UserContext.getUser();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        Long rank = stringRedisTemplate.opsForZSet().reverseRank(key, userId.toString()) ;
        PointsBoard p = new PointsBoard();
        p.setPoints(score == null ? 0 : score.intValue());
        p.setRank(rank == null ? 0 : rank.intValue() + 1);
        return p;
    }
}
