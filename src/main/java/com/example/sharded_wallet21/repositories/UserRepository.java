package com.example.sharded_wallet21.repositories;

import com.example.sharded_wallet21.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
    boolean existsByRole(com.example.sharded_wallet21.entities.Role role);
}
