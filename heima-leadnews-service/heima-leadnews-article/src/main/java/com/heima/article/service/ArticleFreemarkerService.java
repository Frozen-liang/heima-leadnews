package com.heima.article.service;

import com.heima.model.common.article.entity.ApArticle;
import freemarker.template.TemplateException;

import java.io.IOException;

public interface ArticleFreemarkerService {

    /**
     * 生成静态文件上传到minIO中
     * @param apArticle
     * @param content
     */
     void buildArticleToMinIO(ApArticle apArticle, String content) throws IOException, TemplateException;
}