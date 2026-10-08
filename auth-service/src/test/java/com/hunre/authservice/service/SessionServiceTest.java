package com.hunre.authservice.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SessionServiceTest {
    @ParameterizedTest
    @CsvSource({
            "Windows Chrome/130 Safari/537 Edg/130, Edge trên Windows",
            "Android Chrome/130 EdgA/130, Edge trên Android",
            "iPhone EdgiOS/130, Edge trên iOS",
            "iPad FxiOS/130, Firefox trên iOS",
            "Android Chrome/130, Chrome trên Android",
            "iPhone CriOS/130, Chrome trên iOS",
            "Macintosh Version/18 Safari/605, Safari trên macOS",
            "iPhone Version/18 Safari/605, Safari trên iOS",
            "Linux Firefox/130, Firefox trên Linux",
            "Windows Chrome/130 OPR/100, Opera trên Windows"
    })
    void identifiesBrowsersBeforeTheirCompatibleTokens(String ua, String label) {
        assertThat(SessionService.device(ua)).isEqualTo(label);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"node", "PostmanRuntime/7", "<script>alert(1)</script>", "Windows unknown", "Chrome/130"})
    void unknownDevicesNeverEchoRawUserAgent(String ua) {
        assertThat(SessionService.device(ua)).isEqualTo("Thiết bị khác");
    }
}
