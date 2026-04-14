package com.example.aicodeproductbackend.core;

import cn.hutool.json.JSONUtil;
import com.example.aicodeproductbackend.ai.AiCodeGeneratorService;
import com.example.aicodeproductbackend.ai.model.HtmlCodeResult;
import com.example.aicodeproductbackend.ai.model.MultiFileCodeResult;
import com.example.aicodeproductbackend.ai.model.message.AiResponseMessage;
import com.example.aicodeproductbackend.ai.model.message.ToolExecutedMessage;
import com.example.aicodeproductbackend.ai.model.message.ToolRequestMessage;
import com.example.aicodeproductbackend.config.AiCodeGeneratorServiceFactory;
import com.example.aicodeproductbackend.contant.AppConstant;
import com.example.aicodeproductbackend.core.builder.VueProjectBuilder;
import com.example.aicodeproductbackend.core.parser.CodeParserExecutor;
import com.example.aicodeproductbackend.core.saver.CodeFileSaverExecutor;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService=aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codeGenTypeEnum) {
            case HTML ->{
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield  CodeFileSaverExecutor.executorSaver(codeGenTypeEnum,htmlCodeResult,appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executorSaver(codeGenTypeEnum,multiFileCodeResult,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId) {

        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService=aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId,codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> htmlCodeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processStream(htmlCodeStream, codeGenTypeEnum,appId);
            }
            case MULTI_FILE -> {
                Flux<String> multiFileCodeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processStream(multiFileCodeStream, codeGenTypeEnum,appId);
            }
            case VUE_PROJECT -> {
                TokenStream vueProjectTokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId,userMessage);
                yield processTokenStream(vueProjectTokenStream,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage,Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService=aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(result);
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage,Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService=aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultipleFileCodeResult(result);
    }

    /**
     * 生成流式响应并保存文件
     * @param userMessage
     * @return
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage,Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService=aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        //获取流式返回结果
        Flux<String> htmlCodeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        StringBuilder stringBuilder = new StringBuilder();
        //拼接流式结果字符串
        return htmlCodeStream.doOnNext(stringBuilder::append)
                .doOnComplete(() -> {
                    try {
                        //正则表达式解析响应结果
                        String builderString = stringBuilder.toString();
                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(builderString);
                        //保存结果路径
                        File fileSave = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                        log.info("保存成功，路径为： {}", fileSave.getAbsolutePath());
                    }catch (Exception e){
                        log.error("保存失败，错误信息为： {}", e.getMessage());
                    }
                });

    }

    /**
     * 生成多文件流式响应并保存文件
     * @param userMessage
     * @return
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage,Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService=aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        //获取流式返回结果
        Flux<String> multiFileCodeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        StringBuilder stringBuilder = new StringBuilder();
        //拼接流式结果字符串
        return multiFileCodeStream.doOnNext(stringBuilder::append)
                .doOnComplete(() -> {
                    try {
                        //正则表达式解析响应结果
                        String builderString = stringBuilder.toString();
                        MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(builderString);
                        //保存多文件结果路径
                        File fileSave = CodeFileSaver.saveMultipleFileCodeResult(multiFileCodeResult);
                        log.info("保存成功，路径为： {}", fileSave.getAbsolutePath());
                    }catch (Exception e){
                        log.error("保存失败，错误信息为： {}", e.getMessage());
                    }
                });

    }

    /**
     * 流式响应保存文件统一入口
     * @param codeStream
     * @param codeGenType
     * @param appId
     * @return
     */
    private Flux<String> processStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType,Long appId) {
        StringBuilder stringBuilder = new StringBuilder();
        //拼接流式结果字符串
        return codeStream.doOnNext(stringBuilder::append)
                .doOnComplete(() -> {
                    try {
                        //正则表达式解析响应结果
                        String builderString = stringBuilder.toString();
                        Object codeResult = CodeParserExecutor.executorParser(builderString, codeGenType);
                        //保存多文件结果路径
                        File fileSave = CodeFileSaverExecutor.executorSaver(codeGenType, codeResult, appId);
                        log.info("保存成功，路径为： {}", fileSave.getAbsolutePath());
                    }catch (Exception e){
                        log.error("保存失败，错误信息为： {}", e.getMessage());
                    }
                });
    }

    /**
     * 流式响应保存文件统一入口
     * @param tokenStream
     * @return 提供转换流式对象响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream,Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse )-> {
                AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            }).onPartialToolExecutionRequest((index,toolExecutionRequest) -> {
                ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                sink.next(JSONUtil.toJsonStr(toolRequestMessage));
            }).onToolExecuted((ToolExecution toolExecution)->{
                ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
            }).onCompleteResponse((chatResponse -> {
                //异步构造vue项目
                String projectPath= AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                vueProjectBuilder.buildProject(projectPath);
                sink.complete();
            })).onError(throwable -> {
                throwable.printStackTrace();
                sink.error(throwable);
            }).start();

        });
    }
}
