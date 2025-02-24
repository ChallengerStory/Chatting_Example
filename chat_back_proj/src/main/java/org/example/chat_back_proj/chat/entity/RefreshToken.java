package org.example.chat_back_proj.chat.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;
    private String username;
    private String token;
    private Instant expiryDate;
} 