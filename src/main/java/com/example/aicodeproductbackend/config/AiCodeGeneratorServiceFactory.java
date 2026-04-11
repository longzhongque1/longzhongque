package com.example.aicodeproductbackend.config;

import com.example.aicodeproductbackend.ai.AiCodeGeneratorService;
import com.example.aicodeproductbackend.ai.tools.*;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;
import com.example.aicodeproductbackend.service.ChatHistoryService;
import com.example.aicodeproductbackend.utils.SpringContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class AiCodeGeneratorServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(AiCodeGeneratorServiceFactory.class);
    @Resource
    private ChatModel chatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;


    private final Cache<String, AiCodeGeneratorService> caffeineBuilder = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除，缓存键:{},原因：{}", key, cause);
            })
            .build();

    /**
     * 兼容原生代码生成
     *
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        //有就从缓存取，没有就加载应用实例到缓存
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.MULTI_FILE);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        //构建缓存键
        String cacheKey = appId + "-" + codeGenTypeEnum.getValue();
        //有就从缓存取，没有就加载应用实例到缓存
        return caffeineBuilder.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenTypeEnum));
    }

    public AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return switch (codeGenTypeEnum) {
            case VUE_PROJECT ->{
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                 yield AiServices.builder(AiCodeGeneratorService.class).
                    chatModel(chatModel).
                    streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from
                            (toolExecutionRequest, "Error: there is no tool" + toolExecutionRequest.name()))
                    .build();
            }
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class).
                    chatModel(chatModel).
                    streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型" + codeGenTypeEnum);
        };
    }

    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

}
