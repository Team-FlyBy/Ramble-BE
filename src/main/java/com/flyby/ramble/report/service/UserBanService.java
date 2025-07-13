package com.flyby.ramble.report.service;

import com.flyby.ramble.report.dto.BanUserCommandDTO;
import com.flyby.ramble.report.model.UserBan;
import com.flyby.ramble.report.repository.UserBanRepository;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBanService {
    private final UserRepository userRepository;
    private final UserBanRepository userBanRepository;

    @Transactional
    public void banUser(BanUserCommandDTO commandDTO) {
        User bannedUser = userRepository.findByExternalId(commandDTO.getUserUuid()).orElseThrow();

        UserBan userBan = UserBan.builder()
                .bannedUser(bannedUser)
                .reason(commandDTO.getBanReason())
                .bannedAt(commandDTO.getBannedAt())
                .banExpiresAt(commandDTO.getBanExpiresAt())
                .build();

        userBanRepository.save(userBan);
    }


}
