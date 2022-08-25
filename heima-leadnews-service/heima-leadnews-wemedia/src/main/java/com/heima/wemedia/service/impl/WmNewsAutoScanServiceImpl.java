package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.exception.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.article.dtos.ArticleDto;
import com.heima.model.common.article.entity.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.wemedia.entity.WmChannel;
import com.heima.model.common.wemedia.entity.WmNews;
import com.heima.model.common.wemedia.entity.WmSensitive;
import com.heima.model.common.wemedia.entity.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     */
    @Override
    @Async
    public void autoScanWmNews(Integer id) {

        // 一 查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("id参数错误！审核自媒体文章不存在！");
        }

        // 二 内容中提取文本和图片
        Map<String, Object> textAndImage = getTextAndImage(wmNews);
        // (判断文章是否为提交状态)
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            // 2.1 审核自定义敏感词汇
            boolean checkSensitiveWnNewsText = checkSensitiveWnNewsText((String) textAndImage.get("content"), wmNews);
            if (!checkSensitiveWnNewsText) return;
            // 2.2审核文本
            checkWnNewsText(wmNews.getContent(), wmNews);

            // 2.审核图片
//            boolean checkWnNewsImage = checkWnNewsImage((List<String>) textAndImage.get("image"), wmNews);
//            if (!checkWnNewsImage) return;

            // 三 审核成功保存文章
            ResponseResult<Long> responseResult = saveArticle(wmNews);

            // 四 自媒体文章回填审核通过相关属性
            wmNews.setArticleId(responseResult.getData());
            updateWmNews(wmNews, WmNews.Status.PUBLISHED.getCode(), "审核通过");
        }

    }

    private Map<String, Object> getTextAndImage(WmNews wmNews) {
        String content = wmNews.getContent();
        // 存储文本内容和图片
        StringBuilder text = new StringBuilder();
        ArrayList<String> images = new ArrayList<>();

        // 提取
        if (StringUtils.isNotBlank(content)) {
            // String 转化为map形式
            List<Map> maps = JSON.parseArray(content, Map.class);
            for (Map map : maps) {
                // 文本
                if (map.get("type").equals("text")) {
                    text.append(map.get("value"));
                }
                // 图片
                if (map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                }
            }
            // 封面图片
            if (StringUtils.isNotBlank(wmNews.getImages())) {
                String[] split = wmNews.getImages().split(",");
                images.addAll(Arrays.asList(split));
            }
        }
        // 返回结果
        HashMap<String, Object> result = new HashMap<>();
        result.put("content", text.toString());
        result.put("image", images);
        return result;
    }

    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    private boolean checkSensitiveWnNewsText(String content, WmNews wmNews) {
        boolean flag = true;

        // 获取库中敏感词汇
        LambdaQueryWrapper<WmSensitive> select = Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives);
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(select);
        // 转化为字符形式集合
        List<String> sensitivesList = wmSensitives.stream()
                .map(WmSensitive::getSensitives)
                .collect(Collectors.toList());
        // 调用DFA算法初始化数据
        SensitiveWordUtil.initMap(sensitivesList);

        // 开始比较
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if (map.size() > 0) {
            updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "存在违规内容：" + map);
            flag = false;
        }
        return flag;
    }

    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }


    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;
    @Autowired
    private IArticleClient iArticleClient;

    /**
     * 保存App文章
     *
     * @param wmNews 自媒体文章
     */
    private ResponseResult<Long> saveArticle(WmNews wmNews) {
        ArticleDto articleDto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, articleDto);
        // 布局
        articleDto.setLayout(wmNews.getType());
        // 频道名字
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        articleDto.setChannelName(wmChannel.getName());
        // 作者
        articleDto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        articleDto.setAuthorName(wmUser.getName());
        // 设置文章id
        if (wmNews.getArticleId() != null) {
            articleDto.setId(wmNews.getArticleId());
        }
        articleDto.setCreatedTime(new Date());

        return iArticleClient.saveArticle(articleDto);
    }

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private Tess4jClient tess4jClient;

    private boolean checkWnNewsImage(List<String> images, WmNews wmNews) {
        boolean flag = true;

        if (images == null) {
            return flag;
        }
        //  去重 下载图片
        images = images.stream().distinct().collect(Collectors.toList());
//        ArrayList<byte[]> imageList = new ArrayList<>();
        try {
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);
                // 2.1审核图片中的文字
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                String text = tess4jClient.ORC(bufferedImage);
                boolean result = checkSensitiveWnNewsText(text, wmNews);
                if (!result) {
                    return result;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("图片审核失败");
        }

        return flag;
    }

    private void checkWnNewsText(String content, WmNews wmNews) {
    }
}

