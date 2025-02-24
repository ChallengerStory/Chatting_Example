package org.example.chat_back_proj.chat.repository;

import org.example.chat_back_proj.chat.entity.ChatMessageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ChatMessageRepository extends MongoRepository<ChatMessageEntity, String> {
    
    // 채팅방의 메시지를 시간순으로 조회 (오래된 메시지부터)
    List<ChatMessageEntity> findByRoomIdOrderByTimestampAsc(String roomId);

    boolean existsByRoomIdAndSenderAndType(String roomId, String sender, ChatMessageEntity.MessageType type);

    Optional<ChatMessageEntity> findTopByRoomIdAndSenderAndTypeOrderByTimestampDesc(
        String roomId, String sender, ChatMessageEntity.MessageType type);

    List<ChatMessageEntity> findByRoomIdAndTimestampAfterOrderByTimestampAsc(
        String roomId, LocalDateTime timestamp);
} 