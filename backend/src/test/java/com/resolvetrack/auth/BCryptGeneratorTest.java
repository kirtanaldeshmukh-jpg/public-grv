package com.resolvetrack.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BCryptGeneratorTest {

    @Test
    void generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String adminHash = encoder.encode("Admin@123");
        String supervisorHash = encoder.encode("Supervisor@123");
        String officerHash = encoder.encode("Officer@123");
        String dataEntryHash = encoder.encode("DataEntry@123");

        System.out.println("ADMIN_HASH=" + adminHash);
        System.out.println("SUPERVISOR_HASH=" + supervisorHash);
        System.out.println("OFFICER_HASH=" + officerHash);
        System.out.println("DATA_ENTRY_HASH=" + dataEntryHash);

        assertTrue(encoder.matches("Admin@123", adminHash));
        assertTrue(encoder.matches("Supervisor@123", supervisorHash));
        assertTrue(encoder.matches("Officer@123", officerHash));
        assertTrue(encoder.matches("DataEntry@123", dataEntryHash));
    }
}

