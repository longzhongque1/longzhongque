package com.example.aicodeproductbackend.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
@Description("生产多个代码文件的结果")
@Data
public class MultiFileCodeResult {
    @Description("生成的html代码")
    private String htmlCode;
    @Description("生成的css代码")
    private String cssCode;
    @Description("生成的js代码")
    private String jsCode;
    @Description("生成代码结果描述")
    private String description;
}
