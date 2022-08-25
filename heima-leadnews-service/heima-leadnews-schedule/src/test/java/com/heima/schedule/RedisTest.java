package com.heima.schedule;

import com.heima.common.exception.redis.CacheService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class RedisTest {

    @Autowired
    private CacheService cacheService;

    @Test
    public void test1(){
        // 添加元素
        Long list_001 = cacheService.lLeftPush("list_001", "hello,redis");
        System.out.println(list_001);
    }
}
