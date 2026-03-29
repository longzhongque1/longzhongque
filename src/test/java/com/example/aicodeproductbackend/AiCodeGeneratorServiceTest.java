package com.example.aicodeproductbackend;

import cn.hutool.core.util.IdUtil;
import com.example.aicodeproductbackend.ai.AiCodeGeneratorService;
import com.example.aicodeproductbackend.ai.model.HtmlCodeResult;
import com.example.aicodeproductbackend.ai.model.MultiFileCodeResult;
import com.example.aicodeproductbackend.core.AiCodeGeneratorFacade;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
class AiCodeGeneratorServiceTest {



    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员鱼皮的工作记录小工具");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个坤坤的搞笑页面，代码不超过50行");
        Assertions.assertNotNull(multiFileCode);
    }


    @Test
    void generateAndSaveCodeStream() {
        long appId = IdUtil.getSnowflakeNextId();
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("做个用户登录，代码不超过100行", CodeGenTypeEnum.MULTI_FILE,appId);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}
