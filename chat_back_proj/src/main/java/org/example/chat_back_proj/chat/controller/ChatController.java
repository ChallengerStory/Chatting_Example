package org.example.chat_back_proj.chat.controller;

import org.example.chat_back_proj.chat.dto.ChatMessage;
import org.example.chat_back_proj.chat.entity.ChatMessageEntity;
import org.example.chat_back_proj.chat.entity.ChatRoom;
import org.example.chat_back_proj.chat.entity.User;
import org.example.chat_back_proj.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessage message,
                          SimpMessageHeaderAccessor headerAccessor) {
        if(ChatMessage.MessageType.ENTER.equals(message.getType())) {
            headerAccessor.getSessionAttributes().put("username", message.getSender());
            headerAccessor.getSessionAttributes().put("roomId", roomId);
            message.setMessage(message.getSender() + "님이 입장하셨습니다.");
        }
        
        chatService.sendMessage(roomId, message);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessage> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam String userId) {
        return chatService.getRoomMessages(roomId, userId);
    }

    @GetMapping("/rooms")
    public List<ChatRoom> getRooms() {
        return chatService.getAllRooms();
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return chatService.getAllUsers();
    }

    @PostMapping("/rooms/create")
    public ChatRoom createRoom(@RequestBody ChatRoom room) {
        return chatService.createRoom(room);
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ChatRoom leaveRoom(@PathVariable String roomId, @RequestParam String userId) {
        return chatService.leaveRoom(roomId, userId);
    }

    @GetMapping("/rooms/{roomId}/isFirstJoin")
    public boolean isFirstJoin(@PathVariable String roomId, @RequestParam String userId) {
        return chatService.isFirstJoin(roomId, userId);
    }

    @PostMapping("/rooms/{roomId}/invite")
    public ChatRoom inviteUsers(@PathVariable String roomId, @RequestBody Map<String, List<String>> request) {
        List<String> userIds = request.get("userIds");
        return chatService.inviteUsers(roomId, userIds);
    }
}
