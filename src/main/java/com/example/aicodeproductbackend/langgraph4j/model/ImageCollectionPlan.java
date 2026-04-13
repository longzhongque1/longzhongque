package com.example.aicodeproductbackend.langgraph4j.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ImageCollectionPlan implements Serializable {
    
    /**
     * 内容图片搜索任务列表
     */
    private List<ImageSearchTask> contentImageTasks;

    /**
     * Logo生成任务列表
     */
    private List<LogoTask> logoTasks;
    
    /**
     * 内容图片搜索任务
     * 对应 ImageSearchTool.searchContentImages(String query)
     */
    public record ImageSearchTask(String query) implements Serializable {}

    /**
     * Logo生成任务
     * 对应 LogoGeneratorTool.generateLogos(String description)
     */
    public record LogoTask(String description) implements Serializable {}
}
