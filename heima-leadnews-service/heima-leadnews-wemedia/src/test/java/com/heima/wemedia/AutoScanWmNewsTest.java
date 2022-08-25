package com.heima.wemedia;

import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class AutoScanWmNewsTest {
    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Test
    public void text1() {
        wmNewsAutoScanService.autoScanWmNews(6246);
    }
}
