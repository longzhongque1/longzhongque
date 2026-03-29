package com.example.aicodeproductbackend.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
@Description("生成html代码文件结果")
@Data
public class HtmlCodeResult {

    @Description("生成的html代码")
    private String htmlCode;

    @Description("生成代码的描述")
    private String description;
}
