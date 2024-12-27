package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author bob
 * @since 2024-12-25
 */
@RestController
@Api(tags = "优惠券管理接口")
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {
private final ICouponService couponService;
    @ApiOperation("新增优惠券")
    @PostMapping
    public void addCoupon(@RequestBody CouponFormDTO dto) {
        couponService.addCoupon(dto);
    }
    @ApiOperation("分页查询优惠券")
    @GetMapping("/page")
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query){
        return couponService.queryCouponByPage(query);
    }
    @ApiOperation("发放优惠券接口")
    @PutMapping("/{id}/issue")
    public void beginIssue(@PathVariable Long id,@RequestBody @Validated CouponIssueFormDTO dto) {
        couponService.beginIssue(dto);
    }
    @ApiOperation("查询发放中的优惠券列表")
    @GetMapping("/list")
    public List<CouponVO> queryIssuingCoupons(){
        return couponService.queryIssuingCoupons();
    }


}
