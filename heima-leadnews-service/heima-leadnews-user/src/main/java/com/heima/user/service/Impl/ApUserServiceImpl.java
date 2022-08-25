package com.heima.user.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustomException;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.common.user.dtos.LoginDto;
import com.heima.model.common.user.entity.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;

@Service
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {
    @Override
    public HashMap<String, Object> login(LoginDto loginDto) {

        String phone = loginDto.getPhone();
        String password = loginDto.getPassword();
        HashMap<String, Object> map = new HashMap<>();

        // 正常登录
        // 判断参数是否为空
        if (StringUtils.isNotBlank(phone) && StringUtils.isNotBlank(password)) {
            // 数据库中查询用户
            ApUser apUser = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, phone));
            if (apUser == null) {
                throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
            }
            // 不为空 判断密码
            String salt = apUser.getSalt();
            String truePw = DigestUtils.md5DigestAsHex((password + salt).getBytes());
            if (!truePw.equals(apUser.getPassword())) {
                throw new CustomException(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
            }
            // 返回数据 jwt
            // 获取用户id值
            map.put("token", AppJwtUtil.getToken(apUser.getId().longValue()));
            // 密码和盐值不返回给前端
            apUser.setSalt("");
            apUser.setPassword("");
            map.put("user", apUser);
            return map;
        } else {
            // 游客 id设定为0
            map.put("token", AppJwtUtil.getToken(0L));
            return map;
        }
    }
}

