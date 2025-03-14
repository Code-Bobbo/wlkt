package com.tianji.api.client.remark;


import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(value = "remark-service",fallbackFactory = RemarkClientFallback.class) //被调用方的服务名字
public interface RemarkClient {


    @GetMapping("likes/list")
    public Set<Long> getLikedStatusByBizIds(@RequestParam List<Long> bizIds);

}
