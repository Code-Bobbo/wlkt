package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {

    @Override //如果remark服务没启动，或者其他服务调用remark服务超时，则走create降级
    public RemarkClient create(Throwable cause) {
        log.error("查询学习服务异常", cause);
        return new RemarkClient() {
            @Override
            public Set<Long> getLikedStatusByBizIds(List<Long> bizIds) {
                return null;
            }
        };


    }
}