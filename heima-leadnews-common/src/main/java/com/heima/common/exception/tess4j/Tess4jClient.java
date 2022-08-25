package com.heima.common.exception.tess4j;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tess4j")
public class Tess4jClient {
    private String dataPath;
    private String language;

    public String ORC(BufferedImage bufferedImage) throws TesseractException {
        // 创建对象
        Tesseract tesseract = new Tesseract();
        // 设置路径
        tesseract.setDatapath(dataPath);
        // 设置语言
        tesseract.setLanguage(language);
        // 识别
        String imageText = tesseract.doOCR(bufferedImage);
        // 替换回车和tab键
        return imageText.replaceAll("\\r|\\n", "-").replaceAll(" ", "");
    }
}
