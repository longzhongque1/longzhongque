package com.example.aicodeproductbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatHistoryVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息
     */
    private String message;

    /**
     * 消息类型：user/ai
     */
    private String messageType;

    /**
     * 消息类型文本
     */
    private String messageTypeText;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 消息创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
