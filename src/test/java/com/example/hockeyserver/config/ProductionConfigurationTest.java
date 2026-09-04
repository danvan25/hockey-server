package com.example.hockeyserver.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductionConfigurationTest {

    @Test
    void productionProfileShouldUseSafeServerDefaults() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream(
                "/application-prod.properties"
        )) {
            assertNotNull(input);
            properties.load(input);
        }

        assertEquals("127.0.0.1", properties.getProperty("server.address"));
        assertEquals("false", properties.getProperty("spring.jpa.show-sql"));
        assertEquals("false", properties.getProperty("spring.jpa.open-in-view"));
        assertEquals(
                "${WEBSOCKET_ALLOWED_ORIGINS}",
                properties.getProperty(
                        "security.websocket.allowed-origin-patterns"
                )
        );
    }
}
