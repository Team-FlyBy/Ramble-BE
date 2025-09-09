package com.flyby.ramble.common.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Service
public class GeoIpService {

    private DatabaseReader databaseReader;

    @PostConstruct
    private void init() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/geo/GeoLite2-City.mmdb")) {
            if (is == null) {
                throw new IllegalStateException("GeoLite2 database not found at /geo/GeoLite2-City.mmdb");
            }

            databaseReader = new DatabaseReader.Builder(is).build();
        }
    }

    @PreDestroy
    private void destroy() throws IOException {
        if (databaseReader != null) {
            databaseReader.close();
        }
    }

    @Cacheable(cacheNames = "ipRegion", key = "#ip", unless = "#result == null || #result == 'UNKNOWN'")
    public String getCountryCode(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = databaseReader.city(ipAddress);

            return response.getCountry().getIsoCode();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

}
