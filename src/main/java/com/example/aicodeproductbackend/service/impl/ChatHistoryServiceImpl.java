package com.example.aicodeproductbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.exception.ThrowUtils;
import com.example.aicodeproductbackend.mapper.AppMapper;
import com.example.aicodeproductbackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.aicodeproductbackend.model.entity.ChatHistory;
import com.example.aicodeproductbackend.mapper.ChatHistoryMapper;
import com.example.aicodeproductbackend.model.entity.App;
import com.example.aicodeproductbackend.model.entity.User;
import com.example.aicodeproductbackend.model.enums.ChatHistoryMessageTypeEnum;
import com.example.aicodeproductbackend.model.enums.UserRoleEnum;
import com.example.aicodeproductbackend.model.vo.ChatHistoryVO;
import com.example.aicodeproductbackend.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * 对话历史 服务层实现。
 *
 * @author Fairytail
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

	@Resource
	private AppMapper appMapper;

	@Override
	public void saveUserMessage(Long appId, Long userId, String message) {
		saveMessage(appId, userId, message, ChatHistoryMessageTypeEnum.USER.getValue());
	}

	@Override
	public void saveAiMessage(Long appId, Long userId, String message) {
		saveMessage(appId, userId, message, ChatHistoryMessageTypeEnum.AI.getValue());
	}

	@Override
	public void saveAiErrorMessage(Long appId, Long userId, String errorMessage) {
		String formattedError = String.format("[ERROR] %s", StrUtil.blankToDefault(errorMessage, "AI 回复失败"));
		saveMessage(appId, userId, formattedError, ChatHistoryMessageTypeEnum.AI.getValue());
	}

	@Override
	public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
		try {
			// 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
			QueryWrapper queryWrapper = QueryWrapper.create()
					.eq(ChatHistory::getAppId, appId)
					.orderBy(ChatHistory::getCreateTime, false)
					.limit(1, maxCount);
			List<ChatHistory> historyList = this.list(queryWrapper);
			if (CollUtil.isEmpty(historyList)) {
				return 0;
			}
			// 反转列表，确保按时间正序（老的在前，新的在后）
			historyList = historyList.reversed();
			// 按时间顺序添加到记忆中
			int loadedCount = 0;
			// 先清理历史缓存，防止重复加载
			chatMemory.clear();
			for (ChatHistory history : historyList) {
				if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
					chatMemory.add(UserMessage.from(history.getMessage()));
					loadedCount++;
				} else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
					chatMemory.add(AiMessage.from(history.getMessage()));
					loadedCount++;
				}
			}
			log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
			return loadedCount;
		} catch (Exception e) {
			log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
			// 加载失败不影响系统运行，只是没有历史上下文
			return 0;
		}
	}


	@Override
	public Page<ChatHistoryVO> listAppChatHistoryByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser) {
		ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
		Long appId = chatHistoryQueryRequest.getAppId();
		ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 不能为空");
		ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
		checkAppQueryAuth(appId, loginUser);

		long pageSize = chatHistoryQueryRequest.getPageSize();
		if (pageSize <= 0) {
			pageSize = 10;
		}
		ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每次最多加载 20 条消息");

		QueryWrapper queryWrapper = buildQueryWrapper(chatHistoryQueryRequest);
		Page<ChatHistory> historyPage = this.page(Page.of(1, pageSize), queryWrapper);
		List<ChatHistoryVO> chatHistoryVOList = getChatHistoryVOList(historyPage.getRecords());
		// 结果按时间升序返回，便于前端直接展示对话顺序
		//Collections.reverse(chatHistoryVOList);

		Page<ChatHistoryVO> pageResult = new Page<>(1, pageSize, historyPage.getTotalRow());
		pageResult.setRecords(chatHistoryVOList);
		return pageResult;
	}

	@Override
	public Page<ChatHistoryVO> listChatHistoryByPageForAdmin(ChatHistoryQueryRequest chatHistoryQueryRequest) {
		ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
		long pageNum = chatHistoryQueryRequest.getPageNum();
		long pageSize = chatHistoryQueryRequest.getPageSize();
		ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页号错误");
		ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "分页大小错误");

		QueryWrapper queryWrapper = buildQueryWrapper(chatHistoryQueryRequest);
		Page<ChatHistory> historyPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
		Page<ChatHistoryVO> pageResult = new Page<>(pageNum, pageSize, historyPage.getTotalRow());
		pageResult.setRecords(getChatHistoryVOList(historyPage.getRecords()));
		return pageResult;
	}

	@Override
	public boolean removeByAppId(Long appId) {
		ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 不能为空");
		QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
		return this.remove(queryWrapper);
	}

	private void saveMessage(Long appId, Long userId, String message, String messageType) {
		ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 不能为空");
		ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "userId 不能为空");
		ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
		ThrowUtils.throwIf(ChatHistoryMessageTypeEnum.getEnumByValue(messageType) == null,
				ErrorCode.PARAMS_ERROR, "消息类型不合法");

		ChatHistory chatHistory = new ChatHistory();
		chatHistory.setAppId(appId);
		chatHistory.setUserId(userId);
		chatHistory.setMessage(message);
		chatHistory.setMessageType(messageType);
		boolean saveResult = this.save(chatHistory);
		ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "保存对话历史失败");
	}

	private QueryWrapper buildQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
		QueryWrapper queryWrapper = QueryWrapper.create();
		queryWrapper.eq("id", chatHistoryQueryRequest.getId());
		queryWrapper.eq("appId", chatHistoryQueryRequest.getAppId());
		queryWrapper.eq("userId", chatHistoryQueryRequest.getUserId());
		queryWrapper.eq("messageType", chatHistoryQueryRequest.getMessageType());
		queryWrapper.like("message", chatHistoryQueryRequest.getMessage());
		if (chatHistoryQueryRequest.getLastCreateTime() != null) {
			queryWrapper.lt("createTime", chatHistoryQueryRequest.getLastCreateTime());
		}
		if (StrUtil.isNotBlank(chatHistoryQueryRequest.getSortField())){
			queryWrapper.orderBy(chatHistoryQueryRequest.getSortField(),"ascend".equals(chatHistoryQueryRequest.getSortOrder()));
        } else {
            queryWrapper.orderBy("createTime",false);
		}
		return queryWrapper;
	}

	private void checkAppQueryAuth(Long appId, User loginUser) {
		App app = appMapper.selectOneById(appId);
		ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
		boolean isAppOwner = app.getUserId().equals(loginUser.getId());
		boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
		if (!isAppOwner && !isAdmin) {
			throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限查看该应用的对话历史");
		}
	}

	private List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
		if (CollUtil.isEmpty(chatHistoryList)) {
			return new ArrayList<>();
		}
		return chatHistoryList.stream().map(this::getChatHistoryVO).toList();
	}

	private ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
		if (chatHistory == null) {
			return null;
		}
		ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
		BeanUtil.copyProperties(chatHistory, chatHistoryVO);
		ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(chatHistory.getMessageType());
		if (messageTypeEnum != null) {
			chatHistoryVO.setMessageTypeText(messageTypeEnum.getText());
		}
		return chatHistoryVO;
	}

}
