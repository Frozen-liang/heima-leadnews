package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.common.wemedia.dots.WmMaterialDto;
import com.heima.model.common.wemedia.entity.WmMaterial;
import com.heima.utils.common.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public ResponseResult findList(WmMaterialDto dto) {

        // 检查参数
        dto.checkParam();

        // 分页对象
        IPage<WmMaterial> page = new Page<>(dto.getPage(), dto.getSize());
        // 条件查询
        LambdaQueryWrapper<WmMaterial> lqw = new LambdaQueryWrapper<>();

        // 判断是否收藏
        Short isCollection = dto.getIsCollection();
        if (isCollection != null && isCollection == 1) {
            lqw.eq(WmMaterial::getIsCollection, isCollection);
        }
        // 使用者所属的图片
        lqw.eq(WmMaterial::getUserId, WmThreadLocalUtil.getUser().getId());
        // 排序最新时间 倒序
        lqw.orderByDesc(WmMaterial::getCreatedTime);
        // 分页查询
        IPage<WmMaterial> wmMaterialPage = page(page, lqw);
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(wmMaterialPage.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) throws IOException {
        // 判断参数是否正确
        if (multipartFile == null || multipartFile.getSize() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 修改图片名 获取图片类型
        String name = UUID.randomUUID().toString().replace("-", "");
        String originalFilename = multipartFile.getOriginalFilename();
        String imageType = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 上传
        String url = fileStorageService.uploadImgFile("", name + imageType, multipartFile.getInputStream());

        // 保存图片信息
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
        wmMaterial.setUrl(url);
        wmMaterial.setType((short) 0);
        wmMaterial.setIsCollection((short) 0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);
        return ResponseResult.okResult(wmMaterial);
    }


}
