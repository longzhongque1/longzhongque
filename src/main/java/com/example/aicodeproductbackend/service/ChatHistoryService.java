package com.example.aicodeproductbackend.service;

import com.example.aicodeproductbackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.example.aicodeproductbackend.model.entity.User;
import com.example.aicodeproductbackend.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.example.aicodeproductbackend.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * 对话历史 服务层。
 *
 * @author Fairytail
 */
public interface ChatHistoryService extends IService<ChatHistory> {

	void saveUserMessage(Long appId, Long userId, String message);

	void saveAiMessage(Long appId, Long userId, String message);

	void saveAiErrorMessage(Long appId, Long userId, String errorMessage);

	int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

	Page<ChatHistoryVO> listAppChatHistoryByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser);

	Page<ChatHistoryVO> listChatHistoryByPageForAdmin(ChatHistoryQueryRequest chatHistoryQueryRequest);

	boolean removeByAppId(Long appId);

}
