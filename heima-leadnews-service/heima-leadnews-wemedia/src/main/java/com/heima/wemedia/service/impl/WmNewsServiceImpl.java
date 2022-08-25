package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustomException;
import com.heima.common.exception.constants.WemediaConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.common.wemedia.dots.WmNewsDto;
import com.heima.model.common.wemedia.dots.WmNewsPageReqDto;
import com.heima.model.common.wemedia.entity.WmMaterial;
import com.heima.model.common.wemedia.entity.WmNews;
import com.heima.model.common.wemedia.entity.WmNewsMaterial;
import com.heima.model.common.wemedia.entity.WmUser;
import com.heima.utils.common.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmMaterialMapper materialMapper;
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Override
    public ResponseResult findAll(WmNewsPageReqDto dto) {
        // 参数校验
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        WmUser user = WmThreadLocalUtil.getUser();
        // 分页对象
        IPage<WmNews> page = new Page<>(dto.getPage(), dto.getSize());
        // 条件查询
        LambdaQueryWrapper<WmNews> lqw = new LambdaQueryWrapper<>();
        lqw.eq(WmNews::getUserId, user.getId());
        if (dto.getStatus() != null) {
            lqw.eq(WmNews::getStatus, dto.getStatus());
        }
        if (dto.getChannelId() != null) {
            lqw.eq(WmNews::getChannelId, dto.getChannelId());
        }
        if (dto.getBeginPubDate() != null && dto.getEndPubDate() != null) {
            lqw.between(WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        }
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            lqw.like(WmNews::getTitle, dto.getKeyword());
        }

        lqw.orderByDesc(WmNews::getCreatedTime);

        // 分页查询
        IPage<WmNews> wmNewsPage = page(page, lqw);

        // 封装
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) wmNewsPage.getTotal());
        responseResult.setData(wmNewsPage.getRecords());

        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Override
    public ResponseResult submitNews(WmNewsDto dto) throws InvocationTargetException, IllegalAccessException {

        // 判断参数
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 文章操作
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);
        // 设置图片
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            // [1dddfsd.jpg,sdlfjldk.jpg]-->   1dddfsd.jpg,sdlfjldk.jpg
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        // 如果为自动 则设置为空
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }
        // 保存
        saveOrUpdateWmNews(wmNews);

        // 一 草稿判断
        // 1 是草稿不做任何关联 只保存到文章表中
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        // 2 不是草稿 发表文章 文章内容图片作为素材
        // 获取文章中的图片地址
        List<String> materialsUrl = getContentImageUrls(dto.getContent());
        // 通过图片地址 保存文章和素材关系
        saveNewAndMaterialsRelative(wmNews, materialsUrl);

        // 二 用户封面选择判断
        //List<String> images = dto.getImages();
        cover(dto, wmNews, materialsUrl);

        // 文章审核
        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private void cover(WmNewsDto dto, WmNews wmNews, List<String> materialsUrl) {
        // 由于是自动 前端传来封面为空
        List<String> images = dto.getImages();
        // 用户文章封面选择自动
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            if (materialsUrl.size() >= 3) {
                // 文章内容有多图
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materialsUrl.stream().limit(3).collect(Collectors.toList());
            } else if (materialsUrl.size() == 0) {
                // 文章内容无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            } else {
                // 文章内容为单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materialsUrl.stream().limit(1).collect(Collectors.toList());
            }

            // 修改文章
            if (images != null && images.size() > 0) {
                // 设置文章图片地址
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }
        // 保存关系表中
        saveNewAndMaterialsRelative(wmNews, images);
    }

    private void saveNewAndMaterialsRelative(WmNews wmNews, List<String> materialsUrl) {
        if (materialsUrl != null && materialsUrl.size() > 0) {
            // 通过path找到素材id
            List<WmMaterial> materialList = materialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materialsUrl));
            if (materialList == null || materialList.size() == 0) {
                // 抛出异常 数据回滚
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
            }
//            if (materialList.size() != materialsUrl.size()) {
//                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
//            }

            // 在materialList找到id 并且取出来
            List<Integer> idList = materialList.stream().map(WmMaterial::getId).collect(Collectors.toList());
            // 保存文章和素材关系
            wmNewsMaterialMapper.saveRelations(idList, wmNews.getId(), WemediaConstants.WM_CONTENT_REFERENCE);
        }
    }

    private List<String> getContentImageUrls(String content) {
        // content为map集合 转化为集合模式
        List<Map> contentMap = JSON.parseArray(content, Map.class);
        // 创建素材集合装入图片信息imagePath
        List<String> materialsUrl = new ArrayList<>();
        for (Map map : contentMap) {
            // 获取图片key:image
            if (map.get("type").equals("image")) {
                String imagePath = (String) map.get("value");
                materialsUrl.add(imagePath);
            }
        }
        return materialsUrl;
    }

    private void saveOrUpdateWmNews(WmNews wmNews) {
        // 补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1);// 默认上架

        if (wmNews.getId() == null) {
            // 保存操作
            save(wmNews);
        } else {
            // 修改时 要删除文章和素材的操作
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

}