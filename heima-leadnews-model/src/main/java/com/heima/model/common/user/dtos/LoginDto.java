package com.heima.model.common.user.dtos;

import lombok.Data;

@Data
public class LoginDto {
    /**
     * 用户名
     */
    private String phone;
    /**
     * 密码
     */
    private String password;
}
