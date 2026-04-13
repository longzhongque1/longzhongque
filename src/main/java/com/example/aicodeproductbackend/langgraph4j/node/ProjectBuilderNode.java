package com.example.aicodeproductbackend.langgraph4j.node;

import com.example.aicodeproductbackend.core.builder.VueProjectBuilder;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.langgraph4j.state.WorkflowContext;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;
import com.example.aicodeproductbackend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 当前节点一定为Vue项目时才会执行
 */
@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 项目构建");

            // 获取必要的参数
            String generatedCodeDir = context.getGeneratedCodeDir();
            String buildResultDir;
            // Vue 项目类型：使用 VueProjectBuilder 进行构建
                try {
                    VueProjectBuilder vueBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                    // 执行 Vue 项目构建（npm install + npm run build）
                    boolean buildSuccess = vueBuilder.buildProject(generatedCodeDir);
                    if (buildSuccess) {
                        // 构建成功，返回 dist 目录路径
                        buildResultDir = generatedCodeDir + File.separator + "dist";
                        log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
                    } else {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败");
                    }
                } catch (Exception e) {
                    log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                    buildResultDir = generatedCodeDir; // 异常时返回原路径
                }

            // 更新状态
            context.setCurrentStep("Vue项目构建");
            context.setBuildResultDir(buildResultDir);
            log.info("Vue项目构建节点完成，最终目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}

