package com.heima.wemedia;

import com.heima.common.exception.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import net.sourceforge.tess4j.TesseractException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class tess4j {
    @Autowired
    private Tess4jClient tess4jClient;
    @Autowired
    private FileStorageService fileStorageService;

    @Test
    public void test1() throws IOException, TesseractException {
        byte[] bytes = fileStorageService.downLoadFile("http://localhost:9001/leadnews/2022/08/24/7ee1f2b29bae49d29ccea4a521f8ce79.jpeg");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        BufferedImage bufferedImage = ImageIO.read(in);
        String text = tess4jClient.ORC(bufferedImage);
        System.out.println(text);
    }
}
