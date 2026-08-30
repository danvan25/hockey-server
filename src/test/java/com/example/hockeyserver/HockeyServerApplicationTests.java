package com.example.hockeyserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties =
        "security.jwt.secret="
                + "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
)
class HockeyServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
