package org.example.chat_back_proj.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {
    private String roomId;      // 채팅방 번호
    private String sender;      // 보내는 사람
    private String message;     // 메시지 내용
    private MessageType type;   // 메시지 타입

    public enum MessageType {
        CHAT,   // 일반 채팅
        ENTER,  // 입장
        LEAVE   // 퇴장
    }
}
