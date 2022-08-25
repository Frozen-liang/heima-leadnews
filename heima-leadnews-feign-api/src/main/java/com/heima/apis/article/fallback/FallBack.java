package com.heima.apis.article.fallback;

import com.heima.apis.article.IArticleClient;
import com.heima.model.common.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.springframework.stereotype.Component;

/**
 * feign失败配置
 */
//@Component
//public class FallBack implements IArticleClient {
//    @Override
//    public ResponseResult<Long> saveArticle(ArticleDto articleDto) {
//        return ResponseResult.errorResult(AppHttpCodeEnum.NEED_WAIT);
//    }
//}
