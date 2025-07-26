package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.BanUserCommandDTO;
import com.flyby.ramble.report.model.UserBan;
import com.flyby.ramble.report.repository.UserBanRepository;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserBanService {
    private final UserRepository userRepository;
    private final UserBanRepository userBanRepository;

    @Transactional
    public void banUser(BanUserCommandDTO commandDTO) {
        User bannedUser = userRepository.findById(commandDTO.getUserId()).orElseThrow();

        // TODO: DB 시간으로 변경
        UserBan userBan = UserBan.builder()
                .bannedUser(bannedUser)
                .reason(commandDTO.getBanReason())
                .bannedAt(commandDTO.getBannedAt())
                .banExpiresAt(LocalDateTime.now().plusDays(commandDTO.getBanPeriodDays()))
                .build();

        userBanRepository.save(userBan);
    }

    public boolean isUserCurrentlyBanned(Long userId) {
        return userBanRepository.isUserCurrentlyBanned(userId);
    }


    public long countByBannedUser(Long bannedUserId) {
        User bannedUser = userRepository.findById(bannedUserId).orElseThrow();
        return userBanRepository.countByBannedUser(bannedUser);
    }
}
