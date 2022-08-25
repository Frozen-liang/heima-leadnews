package com.heima.wemedia;

import com.heima.common.exception.aliyun.GreenImageScan;
import com.heima.common.exception.aliyun.GreenTextScan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class AliyunTest {
    @Autowired
    private GreenTextScan greenTextScan;
    @Autowired
    private GreenImageScan greenImageScan;

    @Test
    public void textTest() throws Exception {
        Map map1 = greenTextScan.greeTextScan("我是梁杰栋！");
        System.out.println(map1);
        Map map2 = greenTextScan.greeTextScan("我是梁杰栋！冰毒");
        System.out.println(map2);
    }
}
