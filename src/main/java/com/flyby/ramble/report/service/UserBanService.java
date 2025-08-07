package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.BanUserCommandDTO;
import com.flyby.ramble.report.model.BanReason;
import com.flyby.ramble.report.model.UserBan;
import com.flyby.ramble.report.repository.UserBanRepository;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.flyby.ramble.report.constants.Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBanService {
    private final UserRepository userRepository;
    private final UserBanRepository userBanRepository;

    @Transactional
    public void banUser(BanUserCommandDTO commandDTO) {
        User bannedUser = userRepository.findById(commandDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + commandDTO.getUserId()));

        long banCount = countByBannedUser(bannedUser.getId());
        long banPeriodDays = FIRST_BAN_PERIOD_DAYS;

        if (banCount == 1L) {
            banPeriodDays = SECOND_BAN_PERIOD_DAYS;
        } else if (banCount >= 2L) {
            banPeriodDays = THIRD_BAN_PERIOD_DAYS;
        }
        // TODO: DB 시간으로 변경
        UserBan userBan = UserBan.builder()
                .bannedUser(bannedUser)
                .reason(commandDTO.getBanReason())
                .bannedAt(commandDTO.getBannedAt())
                .banExpiresAt(LocalDateTime.now().plusDays(banPeriodDays))
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
