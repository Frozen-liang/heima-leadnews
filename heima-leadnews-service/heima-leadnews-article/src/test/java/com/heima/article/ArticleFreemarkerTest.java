package com.heima.article;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.article.entity.ApArticle;
import com.heima.model.common.article.entity.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Wrapper;
import java.util.HashMap;

@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class ArticleFreemarkerTest {
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    @Autowired
    private FileStorageService fileStorageService;
    // freemarker配置
    @Autowired
    private Configuration configuration;
    @Autowired
    private ApArticleMapper articleMapper;
    @Test
    public void createStaticUrlTest() throws Exception {
        // 获取文章内容
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.
                <ApArticleContent>lambdaQuery().
                eq(ApArticleContent::getArticleId, 1383827787629252610L));
        if (apArticleContent != null&& StringUtils.isNotBlank(apArticleContent.getContent())){

            // 集合装入内容 前端键值为content
            HashMap<String, Object> map = new HashMap<>();
            // 由于内容为String 转化为数组让模板去遍历
            map.put("content", JSON.parseArray(apArticleContent.getContent()));

            // 创建生成文件对象 和流
            Template template = configuration.getTemplate("article.ftl");
            StringWriter writer = new StringWriter();
            // 生成文件
            template.process(map,writer);

            // 上传到minio中
            ByteArrayInputStream bs = new ByteArrayInputStream(writer.toString().getBytes());
            String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", bs);
            System.out.println(path);
            // 修改表ap_article
            ApArticle apArticle = new ApArticle();
            apArticle.setId(apArticleContent.getArticleId());
            apArticle.setStaticUrl(path);
            articleMapper.updateById(apArticle);
//            ApArticle article = ApArticle.builder().
//                    id(apArticleContent.getArticleId()).
//                    staticUrl(path).
//                    build();


        }
    }
}
