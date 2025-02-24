package org.example.chat_back_proj.chat.config;

import org.example.chat_back_proj.chat.entity.ChatMessageEntity;
import org.example.chat_back_proj.chat.entity.ChatRoom;
import org.example.chat_back_proj.chat.entity.User;
import org.example.chat_back_proj.chat.repository.ChatMessageRepository;
import org.example.chat_back_proj.chat.repository.ChatRoomRepository;
import org.example.chat_back_proj.chat.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class MongoDBInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository,
                         ChatRoomRepository chatRoomRepository,
                         ChatMessageRepository chatMessageRepository,
                         PasswordEncoder passwordEncoder) {
        return args -> {
            // 기존 데이터 삭제
            userRepository.deleteAll();
            chatRoomRepository.deleteAll();
            chatMessageRepository.deleteAll();

            // 사용자 더미 데이터 생성
            User user1 = new User();
            user1.setUserId("hong");           // 로그인 ID
            user1.setUsername("홍길동");        // 표시명
            user1.setPassword(passwordEncoder.encode("password123"));
            user1.setMemberNo("M230001");      // 회원번호

            User user2 = new User();
            user2.setUserId("kim");
            user2.setUsername("김철수");
            user2.setPassword(passwordEncoder.encode("password456"));
            user2.setMemberNo("M230002");

            User user3 = new User();
            user3.setUserId("lee");
            user3.setUsername("이영희");
            user3.setPassword(passwordEncoder.encode("password789"));
            user3.setMemberNo("M230003");

            userRepository.saveAll(Arrays.asList(user1, user2, user3));

            // 채팅방 더미 데이터 생성 (username 대신 userId 사용)
            ChatRoom room1 = new ChatRoom();
            room1.setId("room1");
            room1.setName("일반 채팅방");
            room1.setParticipants(Arrays.asList(user1.getUserId(), user2.getUserId()));

            ChatRoom room2 = new ChatRoom();
            room2.setId("room2");
            room2.setName("개발자 채팅방");
            room2.setParticipants(Arrays.asList(user2.getUserId(), user3.getUserId()));

            chatRoomRepository.saveAll(Arrays.asList(room1, room2));

            // 채팅 메시지 더미 데이터 생성
            List<ChatMessageEntity> messages = Arrays.asList(
                createMessage("room1", "홍길동", "안녕하세요!", LocalDateTime.now().minusHours(2)),
                createMessage("room1", "김철수", "네, 안녕하세요!", LocalDateTime.now().minusHours(1)),
                createMessage("room1", "홍길동", "오늘 날씨가 좋네요.", LocalDateTime.now().minusMinutes(30)),
                createMessage("room2", "김철수", "개발자 채팅방에 오신 것을 환영합니다!", LocalDateTime.now().minusDays(1)),
                createMessage("room2", "이영희", "반갑습니다~", LocalDateTime.now().minusDays(1).plusHours(1)),
                createMessage("room2", "김철수", "프로젝트 진행상황 공유해주세요.", LocalDateTime.now().minusHours(5))
            );

            chatMessageRepository.saveAll(messages);
        };
    }

    private ChatMessageEntity createMessage(String roomId, String sender, String message, LocalDateTime timestamp) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setRoomId(roomId);
        entity.setSender(sender);
        entity.setMessage(message);
        entity.setTimestamp(timestamp);
        entity.setType(ChatMessageEntity.MessageType.CHAT);
        return entity;
    }
} 