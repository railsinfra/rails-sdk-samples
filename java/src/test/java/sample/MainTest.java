package sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void normalizeBaseUrlAddsHttpsAndTrailingSlash() {
        assertEquals("https://api.example.com/", Main.normalizeBaseUrl("api.example.com"));
        assertEquals("https://api.example.com/", Main.normalizeBaseUrl("https://api.example.com"));
        assertEquals("https://api.example.com/", Main.normalizeBaseUrl("https://api.example.com/"));
    }

    @Test
    void normalizeBaseUrlEmptyUsesDefault() {
        assertEquals("https://api.railsinfra.com/", Main.normalizeBaseUrl(""));
        assertEquals("https://api.railsinfra.com/", Main.normalizeBaseUrl("   "));
    }
}
