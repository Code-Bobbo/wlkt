package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 签到相关记录 前端控制器
 * </p>
 *
 * @author bob
 * @since 2024-12-21
 */
@RestController
@Api(tags = "签到相关记录")
@RequestMapping("/sign-records")
@RequiredArgsConstructor
public class SignRecordsController {

    private final ISignRecordService signRecordService;
    @ApiOperation("签到")
    @PostMapping
    public SignResultVO sign() {
        return signRecordService.sign();
    }

    @GetMapping
    @ApiOperation("查询m每个月签到结果")
    public Byte[] querySignResult() {
        return signRecordService.querySignResult();
    }

}
