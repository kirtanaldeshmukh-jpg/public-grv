package com.resolvetrack;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class FlywayMigrationTest {

    @Test
    @DisplayName("Verify Flyway migrations V1, V2, V3, and V4 apply cleanly and create all tables, columns, and settings")
    void testFlywayMigrations_CleanRun() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:flywaydb_v4;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        int appliedMigrations = flyway.migrate().migrationsExecuted;
        assertTrue(appliedMigrations >= 4, "Expected at least 4 migrations (V1, V2, V3, V4) executed");

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Verify 5 seed departments
            ResultSet rsDept = stmt.executeQuery("SELECT COUNT(*) FROM departments");
            assertTrue(rsDept.next());
            assertEquals(5, rsDept.getInt(1), "Expected exactly 5 seed departments");

            // 2. Verify admin user exists
            ResultSet rsAdmin = stmt.executeQuery("SELECT role, full_name FROM users WHERE username = 'admin'");
            assertTrue(rsAdmin.next());
            assertEquals("ADMIN", rsAdmin.getString(1));

            // 3. Verify 2 supervisors
            ResultSet rsSuper = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'SUPERVISOR'");
            assertTrue(rsSuper.next());
            assertEquals(2, rsSuper.getInt(1));

            // 4. Verify 5 officers
            ResultSet rsOfficers = stmt.executeQuery("SELECT COUNT(*) FROM officers");
            assertTrue(rsOfficers.next());
            assertEquals(5, rsOfficers.getInt(1));

            // 5. Verify system settings (including category SLAs from V3 and engine settings from V4)
            ResultSet rsSettings = stmt.executeQuery("SELECT COUNT(*) FROM system_settings");
            assertTrue(rsSettings.next());
            assertTrue(rsSettings.getInt(1) >= 16, "Expected at least 16 settings seeded across V1-V4");

            // 6. Verify complaints table has tracking_id, follow_up_deadline, status, source, and health_score columns
            ResultSet rsComplaintCols = stmt.executeQuery("SELECT tracking_id, status, follow_up_deadline, assigned_officer_id, source, health_score FROM complaints WHERE 1=0");
            assertNotNull(rsComplaintCols.getMetaData());

            // 7. Verify tracking_sequences table exists
            ResultSet rsSeq = stmt.executeQuery("SELECT sequence_year, current_sequence FROM tracking_sequences WHERE sequence_year = 2026");
            assertTrue(rsSeq.next());

            // 8. Verify complaint_attachments table exists
            ResultSet rsAtt = stmt.executeQuery("SELECT original_filename, file_path, file_size FROM complaint_attachments WHERE 1=0");
            assertNotNull(rsAtt.getMetaData());

            // 9. Verify notifications table exists
            ResultSet rsNotif = stmt.executeQuery("SELECT id, user_id, title, is_read FROM notifications WHERE 1=0");
            assertNotNull(rsNotif.getMetaData());
        }
    }
}
