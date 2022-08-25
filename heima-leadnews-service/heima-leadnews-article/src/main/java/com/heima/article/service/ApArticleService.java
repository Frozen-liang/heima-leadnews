package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.article.dtos.ArticleDto;
import com.heima.model.common.article.dtos.ArticleHomeDto;
import com.heima.model.common.article.entity.ApArticle;
import com.heima.model.common.dtos.ResponseResult;

import java.util.List;

public interface ApArticleService extends IService<ApArticle> {

    /**
     * 根据参数加载文章列表
     *
     * @param loadType 1为加载更多  2为加载最新
     * @param dto
     * @return
     */
    List<ApArticle> load(Short loadType, ArticleHomeDto dto);

    ResponseResult<Long> saveArticle(ArticleDto articleDto);
}
