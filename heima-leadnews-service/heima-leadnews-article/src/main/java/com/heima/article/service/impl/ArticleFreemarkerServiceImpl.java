package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.article.entity.ApArticle;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

@Service
@Slf4j
@Transactional
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {
    @Autowired
    private FileStorageService fileStorageService;
    // freemarker配置
    @Autowired
    private Configuration configuration;
    @Autowired
    private ApArticleService apArticleService;

    @Override
    @Async
    public void buildArticleToMinIO(ApArticle apArticle, String content) throws IOException, TemplateException {
        // 获取文章内容
        if (StringUtils.isNotBlank(content)) {
            // 集合装入内容 前端键值为content
            HashMap<String, Object> map = new HashMap<>();
            // 由于内容为String 转化为数组让模板去遍历
            map.put("content", JSON.parseArray(content));

            // 创建生成文件对象 和流
            Template template = configuration.getTemplate("article.ftl");
            StringWriter writer = new StringWriter();
            // 生成文件
            template.process(map, writer);

            // 上传到minio中
            ByteArrayInputStream bs = new ByteArrayInputStream(writer.toString().getBytes());
            String path = fileStorageService.uploadHtmlFile("", apArticle.getId() + ".html", bs);
            System.out.println(path);

            // 修改表ap_article中的 static_url字段
            apArticleService.update(Wrappers.<ApArticle>lambdaUpdate()
                    .eq(ApArticle::getId, apArticle.getId())
                    .set(ApArticle::getStaticUrl, path));
        }
    }

}