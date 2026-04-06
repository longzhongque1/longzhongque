package com.example.aicodeproductbackend.config;

import com.example.aicodeproductbackend.ai.AiCodeGeneratorService;
import com.example.aicodeproductbackend.model.entity.ChatHistory;
import com.example.aicodeproductbackend.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
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
    private StreamingChatModel streamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;


    private final Cache<Long, AiCodeGeneratorService> caffeineBuilder = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除，appId:{},原因：{}", key,cause);
            })
            .build();
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        //有就从缓存取，没有就加载应用实例到缓存
        return caffeineBuilder.get(appId, this::createAiCodeGeneratorService);
    }
    public AiCodeGeneratorService createAiCodeGeneratorService(Long appId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId,chatMemory,20);
        return AiServices.builder(AiCodeGeneratorService.class).
                chatModel(chatModel).
                streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

}
