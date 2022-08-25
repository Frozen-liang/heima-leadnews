package com.heima.model.common.article.dtos;

import lombok.Data;

import java.util.Date;

@Data
public class ArticleHomeDto {

    /**
     * 最大时间
     */
    private Date maxBehotTime;
    /**
     * 最小时间
     */
    private Date minBehotTime;
    /**
     * 分页大小
     */
    private Integer size;
    /**
     * 频道
     */
    private String tag;

}
