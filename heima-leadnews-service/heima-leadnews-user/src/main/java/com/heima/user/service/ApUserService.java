package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.user.dtos.LoginDto;
import com.heima.model.common.user.entity.ApUser;

import java.util.HashMap;

public interface ApUserService extends IService<ApUser> {

    HashMap<String, Object> login(LoginDto loginDto);

}
