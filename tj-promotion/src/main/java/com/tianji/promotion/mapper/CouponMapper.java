package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author bob
 * @since 2024-12-25
 */
public interface CouponMapper extends BaseMapper<Coupon> {
    @Update("UPDATE coupon SET issue_num = issue_num + 1 WHERE id = #{couponId}")
    void incrIssueNum(Long id);
}
