package org.example.chat_back_proj.chat.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "messages")
public class ChatMessageEntity {
    @Id
    private String id;
    private String roomId;
    private String sender;
    private String message;
    private LocalDateTime timestamp;
    private MessageType type;

    public enum MessageType {
        CHAT, ENTER, LEAVE
    }
} 