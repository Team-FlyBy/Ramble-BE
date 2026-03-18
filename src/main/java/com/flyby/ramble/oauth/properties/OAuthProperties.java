package com.flyby.ramble.oauth.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "apple")
public class OAuthProperties {

    @NotBlank
    private String clientId;    // iOS Bundle ID

    @NotBlank
    private String serviceId;   // Web Service ID

    @NotBlank
    private String teamId;

    @NotBlank
    private String keyId;

    @NotBlank
    private String privateKey;

}
