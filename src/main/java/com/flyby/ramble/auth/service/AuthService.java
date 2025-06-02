package com.flyby.ramble.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    @CachePut(cacheNames = "jwtBlacklist", key = "#token")
    public boolean putBlackList(String token) {
        return true;
    }

    @Cacheable(cacheNames = "jwtBlacklist", key = "#token", unless = "#result == false")
    public boolean isBlacklisted(String token) {
        return false;
    }

}
