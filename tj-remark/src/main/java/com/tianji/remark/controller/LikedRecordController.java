package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author bob
 * @since 2024-12-18
 */
@RestController
@Api(tags = "点赞记录管理")
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikedRecordController {
    private final ILikedRecordService likedRecordService;

    @PostMapping("")
    @ApiOperation("点赞或者取消")
     public void likeOrCancel(@RequestBody @Validated LikeRecordFormDTO form){
        likedRecordService.likeOrCancel(form);
    }
    @GetMapping("list")
    @ApiOperation("批量查询点赞状态")
    public Set<Long> getLikedStatusByBizIds(@RequestParam List<Long> bizIds){
        return likedRecordService.getLikedStatusByBizIds(bizIds);
    }

}
