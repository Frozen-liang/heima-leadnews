package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.exception.constants.ArticleConstants;
import com.heima.model.common.article.dtos.ArticleDto;
import com.heima.model.common.article.dtos.ArticleHomeDto;
import com.heima.model.common.article.entity.ApArticle;
import com.heima.model.common.article.entity.ApArticleConfig;
import com.heima.model.common.article.entity.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Transactional
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    //设置单页显示文章数
    private static final Integer MAX_PAGE_SIZE = 50;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Override
    public List<ApArticle> load(Short loadType, ArticleHomeDto dto) {

        // 校验参数
        // 判断size大小 设置为10
        Integer size = dto.getSize();
        if (size == null || size == 0) {
            size = 10;
        }
        // 文章频道判断 为空设置为默认频道
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        // 时间校验 为空都设置为罪行时间
        if (dto.getMaxBehotTime() == null)
            dto.setMaxBehotTime(new Date());
        if (dto.getMinBehotTime() == null)
            dto.setMinBehotTime(new Date());
        // 用户参数校验 设置为加载更多
        if (!loadType.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !loadType.equals(ArticleConstants.LOADTYPE_LOAD_NEW))
            loadType = ArticleConstants.LOADTYPE_LOAD_MORE;

        // 查询并返回
        return apArticleMapper.loadArticleList(dto, loadType);
    }

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;

    @Override
    public ResponseResult<Long> saveArticle(ArticleDto articleDto) {

        // 一 校验参数
        if (articleDto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(articleDto, apArticle);

        // 二 判断ID是否存在
        if (articleDto.getId() == null) {
            // 1.不存在 保存文章 文章配置 文章内容
            save(apArticle);

            // 调整构造函数 赋值id 其余默认
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);

            //
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(articleDto.getContent());
            apArticleContentMapper.insert(apArticleContent);
        } else {
            // 2.存在 保存文章 文章内容
            updateById(apArticle);

            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().
                    eq(ApArticleContent::getArticleId, apArticle.getId()));
            apArticleContent.setContent(articleDto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }

        try {
            articleFreemarkerService.buildArticleToMinIO(apArticle, articleDto.getContent());
        } catch (IOException e) {
            throw new RuntimeException("生成文件minio错误");
        } catch (TemplateException e) {
            throw new RuntimeException("生成模板错误！");
        }
        // 返回文章id
        return ResponseResult.okResult(apArticle.getId());
    }
}
