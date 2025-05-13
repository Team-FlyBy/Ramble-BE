package com.flyby.ramble.repository;

import com.flyby.ramble.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
