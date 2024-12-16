package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author bob
 * @since 2024-12-04
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    void save(QuestionFormDTO dto);

    void updateQuestion(Long id, QuestionFormDTO dto);

    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);

    QuestionVO getQuestionById(Long id);

    PageDTO<QuestionAdminVO> queryQuestionAdminPage(QuestionAdminPageQuery query);
}
