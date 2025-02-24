package org.example.chat_back_proj.chat.repository;

import org.example.chat_back_proj.chat.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserId(String userId);
    Optional<User> findByMemberNo(String memberNo);
    boolean existsByUserId(String userId);
    boolean existsByMemberNo(String memberNo);
} 