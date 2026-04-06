package com.example.aicodeproductbackend.controller;

import com.example.aicodeproductbackend.annotation.AuthCheck;
import com.example.aicodeproductbackend.common.BaseResponse;
import com.example.aicodeproductbackend.common.ResultUtils;
import com.example.aicodeproductbackend.contant.UserConstant;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.exception.ThrowUtils;
import com.example.aicodeproductbackend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.example.aicodeproductbackend.model.entity.User;
import com.example.aicodeproductbackend.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.aicodeproductbackend.service.ChatHistoryService;
import com.example.aicodeproductbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史 控制层。
 *
 * @author Fairytail
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private UserService userService;

    /**
     * 分页获取某个应用的聊天历史（仅应用创建者和管理员可查看）
     */
    @PostMapping("/app/list/page/vo")
    public BaseResponse<Page<ChatHistoryVO>> listAppChatHistoryVOByPage(
            @RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistoryVO> pageResult = chatHistoryService.listAppChatHistoryByPage(chatHistoryQueryRequest, loginUser);
        return ResultUtils.success(pageResult);
    }

    /**
     * 管理员分页查看所有对话历史（时间倒序）
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryVOByPageByAdmin(
            @RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<ChatHistoryVO> pageResult = chatHistoryService.listChatHistoryByPageForAdmin(chatHistoryQueryRequest);
        return ResultUtils.success(pageResult);
    }

}
