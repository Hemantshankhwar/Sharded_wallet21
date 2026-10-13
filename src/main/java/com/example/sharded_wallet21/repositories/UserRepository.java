package com.example.sharded_wallet21.repositories;

import com.example.sharded_wallet21.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    List<User> findByName(String name);
}
