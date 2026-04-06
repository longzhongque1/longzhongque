package com.example.aicodeproductbackend.model.dto.chathistory;

import com.example.aicodeproductbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息内容（管理员查询可用）
     */
    private String message;

    /**
     * 消息类型：user/ai
     */
    private String messageType;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 消息创建用户 id
     */
    private Long userId;

    /**
     * 向前加载游标：仅查询该时间之前的消息
     */
    private LocalDateTime lastCreateTime;

    private static final long serialVersionUID = 1L;
}
