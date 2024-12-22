package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author bob
 * @since 2024-11-13
 */
public interface ISignRecordService  {


    SignResultVO sign();

    Byte[] querySignResult();
}
