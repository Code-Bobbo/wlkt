package com.itheima;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.LearningApplication;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.service.ILearningLessonService;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = LearningApplication.class) //只要测试类所在的包和引导类所在的包名字不一样，就得加这个属性
public class MPTest {

    @Autowired
    private ILearningLessonService learningLessonService;
    @Test
    public void test(){
        //构造分页对象
        Page<LearningLesson> page = new Page<>(1,2);
        //构造条件构造器，构建匹配条件和排序字段
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId,2)
                .orderByAsc(LearningLesson::getCreateTime);
        //进行分页查询
        learningLessonService.page(page,wrapper);
        List<LearningLesson> records = page.getRecords();
        for (LearningLesson record :records) {
            System.out.println(record);
        }


    }
    @Test
    public void test1(){
        //构造分页对象
        Page<LearningLesson> page = new Page<>(1,2);
        List<OrderItem> orders = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setColumn("create_time"); //按什么字段排序
        orderItem.setAsc(false);// 升序还是降序
        orders.add(orderItem);
        page.addOrder(orders);
        //构造条件构造器，构建匹配条件和排序字段
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId,2);
        //进行分页查询
        learningLessonService.page(page,wrapper);
        List<LearningLesson> records = page.getRecords();
        for (LearningLesson record :records) {
            System.out.println(record);
        }


    }
    @Test
    public void test2(){
        //构造分页对象
//        Page<LearningLesson> page = new Page<>(1,2);
//        List<OrderItem> orders = new ArrayList<>();
//        OrderItem orderItem = new OrderItem();
//        orderItem.setColumn("create_time"); //按什么字段排序
//        orderItem.setAsc(false);// 升序还是降序
//        orders.add(orderItem);
//        page.addOrder(orders);
//        //构造条件构造器，构建匹配条件和排序字段
//        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(LearningLesson::getUserId,2);
        //进行分页查询
        PageQuery query = new PageQuery();
        query.setPageNo(1);
        query.setPageSize(2);
        query.setSortBy("create_time");
        query.setIsAsc(true);
        //如果前端没给我们传按照那个字段排序，就按照默认排序
        Page<LearningLesson> page = learningLessonService.lambdaQuery().eq(LearningLesson::getUserId, 2).page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        for (LearningLesson record :records) {
            System.out.println(record);
        }


    }

    @Test
    public void test3(){

        learningLessonService.removeUserLessons(2L, CollUtils.singletonList(2L));
    }
    @Test
    public void test4(){
        //查询本周学习计划总数据
        QueryWrapper<LearningLesson> wrapper = new QueryWrapper<>();
        wrapper.select("sum(week_freq) ");//查询哪些列 也可以使用聚合函数，类似于miniob的select部分
        wrapper.eq("user_id", 2);
        wrapper.in("status", LessonStatus.NOT_BEGIN,LessonStatus.LEARNING);
        wrapper.eq("plan_status", PlanStatus.PLAN_RUNNING);
//        Map<String, Object> map = this.getMap(wrapper);
//        Map<String, Object> map = learningLessonService.getMap(wrapper);
        Object obj = learningLessonService.getObj(wrapper,Object::toString);
        System.out.println(obj);

    }

}
