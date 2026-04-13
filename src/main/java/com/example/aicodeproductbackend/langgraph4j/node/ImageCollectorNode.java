package com.example.aicodeproductbackend.langgraph4j.node;

import com.example.aicodeproductbackend.langgraph4j.ImageCategoryEnum;
import com.example.aicodeproductbackend.langgraph4j.ImageResource;
import com.example.aicodeproductbackend.langgraph4j.ai.ImageCollectionPlanService;
import com.example.aicodeproductbackend.langgraph4j.ai.ImageCollectionService;
import com.example.aicodeproductbackend.langgraph4j.model.ImageCollectionPlan;
import com.example.aicodeproductbackend.langgraph4j.state.WorkflowContext;
import com.example.aicodeproductbackend.langgraph4j.tools.ImageSearchTool;
import com.example.aicodeproductbackend.langgraph4j.tools.LogoGeneratorTool;
import com.example.aicodeproductbackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点
 * 使用AI进行工具调用，收集不同类型的图片
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            List<ImageResource> collectImages = new ArrayList<>();
            try {
                // 获取AI图片收集服务
                ImageCollectionPlanService imageCollectionPlanService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                // 使用 AI 服务进行智能图片收集
                ImageCollectionPlan imageCollectionPlan = imageCollectionPlanService.planImageCollection(originalPrompt);
                log.info("图片收集计划并发开始");
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                if (imageCollectionPlan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask imageTask : imageCollectionPlan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(
                    () -> imageSearchTool.searchContentImages(imageTask.query())));}
                }
                if (imageCollectionPlan.getLogoTasks() != null) {
                    LogoGeneratorTool logoGeneratorTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask logoTask : imageCollectionPlan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(
                                () -> logoGeneratorTool.generateLogos(logoTask.description())));
                    }
                }
                // 等待所有任务完成并收集结果
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    if (future.get()!=null) {
                        collectImages.addAll(future.get());
                    }
                }
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
            }
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectImages);
            return WorkflowContext.saveContext(context);
        });
    }
}

