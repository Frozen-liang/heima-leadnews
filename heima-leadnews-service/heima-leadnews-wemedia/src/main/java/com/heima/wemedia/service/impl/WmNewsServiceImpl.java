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
        // ????????????
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        WmUser user = WmThreadLocalUtil.getUser();
        // ????????????
        IPage<WmNews> page = new Page<>(dto.getPage(), dto.getSize());
        // ????????????
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

        // ????????????
        IPage<WmNews> wmNewsPage = page(page, lqw);

        // ??????
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) wmNewsPage.getTotal());
        responseResult.setData(wmNewsPage.getRecords());

        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Override
    public ResponseResult submitNews(WmNewsDto dto) throws InvocationTargetException, IllegalAccessException {

        // ????????????
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // ????????????
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);
        // ????????????
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            // [1dddfsd.jpg,sdlfjldk.jpg]-->   1dddfsd.jpg,sdlfjldk.jpg
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        // ??????????????? ???????????????
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }
        // ??????
        saveOrUpdateWmNews(wmNews);

        // ??? ????????????
        // 1 ??????????????????????????? ????????????????????????
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        // 2 ???????????? ???????????? ??????????????????????????????
        // ??????????????????????????????
        List<String> materialsUrl = getContentImageUrls(dto.getContent());
        // ?????????????????? ???????????????????????????
        saveNewAndMaterialsRelative(wmNews, materialsUrl);

        // ??? ????????????????????????
        //List<String> images = dto.getImages();
        cover(dto, wmNews, materialsUrl);

        // ????????????
        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private void cover(WmNewsDto dto, WmNews wmNews, List<String> materialsUrl) {
        // ??????????????? ????????????????????????
        List<String> images = dto.getImages();
        // ??????????????????????????????
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            if (materialsUrl.size() >= 3) {
                // ?????????????????????
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materialsUrl.stream().limit(3).collect(Collectors.toList());
            } else if (materialsUrl.size() == 0) {
                // ??????????????????
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            } else {
                // ?????????????????????
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materialsUrl.stream().limit(1).collect(Collectors.toList());
            }

            // ????????????
            if (images != null && images.size() > 0) {
                // ????????????????????????
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }
        // ??????????????????
        saveNewAndMaterialsRelative(wmNews, images);
    }

    private void saveNewAndMaterialsRelative(WmNews wmNews, List<String> materialsUrl) {
        if (materialsUrl != null && materialsUrl.size() > 0) {
            // ??????path????????????id
            List<WmMaterial> materialList = materialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materialsUrl));
            if (materialList == null || materialList.size() == 0) {
                // ???????????? ????????????
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
            }
//            if (materialList.size() != materialsUrl.size()) {
//                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
//            }

            // ???materialList??????id ???????????????
            List<Integer> idList = materialList.stream().map(WmMaterial::getId).collect(Collectors.toList());
            // ???????????????????????????
            wmNewsMaterialMapper.saveRelations(idList, wmNews.getId(), WemediaConstants.WM_CONTENT_REFERENCE);
        }
    }

    private List<String> getContentImageUrls(String content) {
        // content???map?????? ?????????????????????
        List<Map> contentMap = JSON.parseArray(content, Map.class);
        // ????????????????????????????????????imagePath
        List<String> materialsUrl = new ArrayList<>();
        for (Map map : contentMap) {
            // ????????????key:image
            if (map.get("type").equals("image")) {
                String imagePath = (String) map.get("value");
                materialsUrl.add(imagePath);
            }
        }
        return materialsUrl;
    }

    private void saveOrUpdateWmNews(WmNews wmNews) {
        // ????????????
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1);// ????????????

        if (wmNews.getId() == null) {
            // ????????????
            save(wmNews);
        } else {
            // ????????? ?????????????????????????????????
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

}