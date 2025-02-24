package org.example.chat_back_proj.chat.repository;

import org.example.chat_back_proj.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
} 