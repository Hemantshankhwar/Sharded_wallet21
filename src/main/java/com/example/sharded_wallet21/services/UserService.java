package com.example.sharded_wallet21.services;

import com.example.sharded_wallet21.entities.User;
import com.example.sharded_wallet21.repositories.UserRepository;
import groovy.util.logging.Slf4j;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@lombok.extern.slf4j.Slf4j
@Service
@Slf4j
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    public User createUser(User user){
        log.info("Creating user:{}", user.getEmail());
        User newUser=userRepository.save(user);
        log.info("User created with id {} in database shardwallet", newUser.getId()%2+1);
        return newUser;
    }

    public User getUserById(Long id) {
        log.info("Fetching user with id {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email == null ? "" : email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user with that email"));
    }

    public List<User> getUsersByName(String name) {
        log.info("Fetching users with name {}", name);
        return userRepository.findByName(name);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

}
