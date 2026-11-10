package com.resolvetrack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResolveTrackApplicationTests {

    @Test
    @DisplayName("Verify ResolveTrackApplication class is loadable")
    void applicationClassPresent() {
        assertNotNull(ResolveTrackApplication.class);
    }
}

