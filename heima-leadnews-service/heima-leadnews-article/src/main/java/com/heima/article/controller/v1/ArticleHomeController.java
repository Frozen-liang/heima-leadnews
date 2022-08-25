package com.heima.article.controller.v1;

import com.heima.article.service.ApArticleService;
import com.heima.common.exception.constants.ArticleConstants;
import com.heima.model.common.article.dtos.ArticleHomeDto;
import com.heima.model.common.article.entity.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/article")
public class ArticleHomeController {

    @Autowired
    private ApArticleService apArticleService;

    @PostMapping("/load")
    public ResponseResult<List<ApArticle>> load(@RequestBody ArticleHomeDto dto) {
        return ResponseResult.okResult(apArticleService.load(ArticleConstants.LOADTYPE_LOAD_MORE, dto));
    }

    @PostMapping("/loadmore")
    public ResponseResult<List<ApArticle>> loadMore(@RequestBody ArticleHomeDto dto) {
        return ResponseResult.okResult(apArticleService.load(ArticleConstants.LOADTYPE_LOAD_MORE, dto));
    }

    @PostMapping("/loadnew")
    public ResponseResult<List<ApArticle>> loadNew(@RequestBody ArticleHomeDto dto) {
        return ResponseResult.okResult(apArticleService.load(ArticleConstants.LOADTYPE_LOAD_NEW, dto));
    }
}
