package com.heima.model.common.article.dtos;

import com.heima.model.common.article.entity.ApArticle;
import lombok.Data;

@Data
public class ArticleDto  extends ApArticle {
    /**
     * 文章内容
     */
    private String content;
}