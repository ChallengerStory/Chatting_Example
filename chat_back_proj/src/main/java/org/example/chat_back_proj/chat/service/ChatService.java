package org.example.chat_back_proj.chat.service;

import org.example.chat_back_proj.chat.dto.ChatMessage;
import org.example.chat_back_proj.chat.entity.ChatMessageEntity;
import org.example.chat_back_proj.chat.repository.ChatMessageRepository;
import org.example.chat_back_proj.chat.repository.ChatRoomRepository;
import org.example.chat_back_proj.chat.repository.UserRepository;
import org.example.chat_back_proj.chat.entity.ChatRoom;
import org.example.chat_back_proj.chat.entity.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendMessage(String roomId, ChatMessage message) {
        // MongoDB에 메시지 저장
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setRoomId(roomId);
        entity.setSender(message.getSender());
        entity.setMessage(message.getMessage());
        entity.setTimestamp(LocalDateTime.now());
        entity.setType(ChatMessageEntity.MessageType.valueOf(message.getType().name()));
        chatMessageRepository.save(entity);

        // WebSocket을 통해 메시지 직접 전송
        messagingTemplate.convertAndSend("/topic/messages/" + roomId, message);

        // RabbitMQ를 통해 메시지 발행 (다중 서버 구성을 위해 유지)
        rabbitTemplate.convertAndSend("chat.exchange", "room." + roomId, message);
    }

    public List<ChatMessage> getRoomMessages(String roomId, String userId) {
        // 사용자의 가장 최근 LEAVE 메시지 시간 조회
        Optional<ChatMessageEntity> lastLeave = chatMessageRepository
            .findTopByRoomIdAndSenderAndTypeOrderByTimestampDesc(
                roomId, userId, ChatMessageEntity.MessageType.LEAVE);
        
        // LEAVE 이후의 메시지만 반환
        if (lastLeave.isPresent()) {
            LocalDateTime leaveTime = lastLeave.get().getTimestamp();
            return chatMessageRepository.findByRoomIdAndTimestampAfterOrderByTimestampAsc(roomId, leaveTime)
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        }
        
        // LEAVE 기록이 없으면 모든 메시지 반환
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ChatMessage convertToDto(ChatMessageEntity entity) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(entity.getRoomId());
        message.setSender(entity.getSender());
        message.setMessage(entity.getMessage());
        message.setType(ChatMessage.MessageType.valueOf(entity.getType().name()));
        return message;
    }

    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public ChatRoom createRoom(ChatRoom room) {
        return chatRoomRepository.save(room);
    }

    public ChatRoom leaveRoom(String roomId, String userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        List<String> participants = new ArrayList<>(room.getParticipants());
        participants.remove(userId);
        room.setParticipants(participants);
        
        // 채팅방을 나가는 시점의 시간을 기록
        LocalDateTime leaveTime = LocalDateTime.now();
        
        // 해당 사용자의 채팅방 나가기 기록 저장
        ChatMessageEntity leaveMessage = new ChatMessageEntity();
        leaveMessage.setRoomId(roomId);
        leaveMessage.setSender(userId);
        leaveMessage.setType(ChatMessageEntity.MessageType.LEAVE);
        leaveMessage.setTimestamp(leaveTime);
        chatMessageRepository.save(leaveMessage);
        
        ChatRoom updatedRoom = chatRoomRepository.save(room);
        
        // 채팅방 정보 업데이트를 모든 참여자에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/update", updatedRoom);
        
        return updatedRoom;
    }

    public boolean isFirstJoin(String roomId, String userId) {
        // 가장 최근의 LEAVE 메시지 시간 조회
        Optional<ChatMessageEntity> lastLeave = chatMessageRepository
            .findTopByRoomIdAndSenderAndTypeOrderByTimestampDesc(
                roomId, userId, ChatMessageEntity.MessageType.LEAVE);
        
        // 가장 최근의 ENTER 메시지 시간 조회
        Optional<ChatMessageEntity> lastEnter = chatMessageRepository
            .findTopByRoomIdAndSenderAndTypeOrderByTimestampDesc(
                roomId, userId, ChatMessageEntity.MessageType.ENTER);
        
        // LEAVE가 없으면 최초 입장
        if (!lastLeave.isPresent()) {
            return !lastEnter.isPresent();
        }
        
        // LEAVE가 있고 ENTER가 없거나, LEAVE가 ENTER보다 더 최근이면 최초 입장으로 처리
        return !lastEnter.isPresent() || 
               lastLeave.get().getTimestamp().isAfter(lastEnter.get().getTimestamp());
    }

    public ChatRoom inviteUsers(String roomId, List<String> userIds) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        List<String> participants = new ArrayList<>(room.getParticipants());
        participants.addAll(userIds);
        room.setParticipants(participants);
        
        ChatRoom updatedRoom = chatRoomRepository.save(room);
        
        // 초대된 사용자들의 입장 메시지 생성 및 저장
        for (String userId : userIds) {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            ChatMessageEntity enterMessage = new ChatMessageEntity();
            enterMessage.setRoomId(roomId);
            enterMessage.setSender(userId);
            enterMessage.setMessage(user.getUsername() + "님이 입장하셨습니다.");
            enterMessage.setType(ChatMessageEntity.MessageType.ENTER);
            enterMessage.setTimestamp(LocalDateTime.now());
            ChatMessageEntity savedMessage = chatMessageRepository.save(enterMessage);
            
            // 입장 메시지를 WebSocket을 통해 전송
            ChatMessage chatMessage = convertToDto(savedMessage);
            messagingTemplate.convertAndSend("/topic/messages/" + roomId, chatMessage);
        }
        
        // 채팅방 정보 업데이트를 모든 참여자에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/update", updatedRoom);
        
        // 새로 초대된 사용자들에게 채팅방 목록 업데이트 알림
        userIds.forEach(userId -> {
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/rooms/update", getAllRooms());
        });
        
        return updatedRoom;
    }
} 