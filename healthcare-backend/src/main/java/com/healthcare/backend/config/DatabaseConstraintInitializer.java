package com.healthcare.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

/**
 * Idempotent, atomic initializer to ensure PostgreSQL appointments_appointment_type_check
 * includes TELECONSULTATION on pre-existing databases where Hibernate ddl-auto=update
 * does not alter existing check constraints.
 *
 * Concurrency & Atomicity:
 * 1. Uses a PostgreSQL session-level advisory lock to serialize concurrent application startups.
 * 2. Uses a single atomic PostgreSQL ALTER TABLE statement combining DROP CONSTRAINT and ADD CONSTRAINT
 *    in a single operation, eliminating any intermediate missing-constraint window.
 * 3. Preserves all existing allowed appointment types (NEW_CONSULTATION, FOLLOW_UP, EMERGENCY, WALK_IN)
 *    and existing appointment rows.
 */
@Component
@Order(1) // Run before DevDataInitializer
@RequiredArgsConstructor
@Slf4j
public class DatabaseConstraintInitializer implements ApplicationRunner {

    public static final long ADVISORY_LOCK_ID = 748291038L;
    public static final String CONSTRAINT_NAME = "appointments_appointment_type_check";
    public static final String TABLE_NAME = "appointments";
    public static final String ROLE_CONSTRAINT_NAME = "roles_name_check";
    public static final String ROLE_TABLE_NAME = "roles";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgres()) {
            return;
        }

        boolean lockAcquired = false;
        try {
            // Acquire advisory lock to coordinate concurrent application replicas
            jdbcTemplate.execute("SELECT pg_advisory_lock(" + ADVISORY_LOCK_ID + ")");
            lockAcquired = true;

            updateAppointmentTypeConstraint();
            updateRoleNameConstraint();

        } catch (Exception ex) {
            log.error("[DatabaseConstraintInitializer] Failed to update constraints: {}", ex.getMessage());
        } finally {
            if (lockAcquired) {
                try {
                    jdbcTemplate.execute("SELECT pg_advisory_unlock(" + ADVISORY_LOCK_ID + ")");
                } catch (Exception unlockEx) {
                    log.warn("[DatabaseConstraintInitializer] Failed to release advisory lock {}: {}", ADVISORY_LOCK_ID, unlockEx.getMessage());
                }
            }
        }
    }

    private void updateAppointmentTypeConstraint() {
        String checkQuery = """
            SELECT pg_get_constraintdef(c.oid)
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            WHERE t.relname = ? AND c.conname = ?
            """;

        List<String> defs = jdbcTemplate.query(checkQuery, (rs, rowNum) -> rs.getString(1), TABLE_NAME, CONSTRAINT_NAME);

        if (defs.isEmpty()) {
            log.info("[DatabaseConstraintInitializer] {} not present; skipping.", CONSTRAINT_NAME);
            return;
        }

        String currentDef = defs.get(0);
        if (currentDef != null && currentDef.contains("TELECONSULTATION")) {
            log.info("[DatabaseConstraintInitializer] {} already permits TELECONSULTATION; no update needed.", CONSTRAINT_NAME);
            return;
        }

        log.info("[DatabaseConstraintInitializer] Atomically updating {} to include TELECONSULTATION...", CONSTRAINT_NAME);

        String atomicUpdateSql = """
            ALTER TABLE appointments
                DROP CONSTRAINT IF EXISTS appointments_appointment_type_check,
                ADD CONSTRAINT appointments_appointment_type_check
                CHECK (appointment_type::text = ANY (ARRAY[
                    'NEW_CONSULTATION'::character varying,
                    'FOLLOW_UP'::character varying,
                    'EMERGENCY'::character varying,
                    'WALK_IN'::character varying,
                    'TELECONSULTATION'::character varying
                ]::text[]))
            """;

        jdbcTemplate.execute(atomicUpdateSql);
        log.info("[DatabaseConstraintInitializer] Successfully updated {} atomically.", CONSTRAINT_NAME);
    }

    private void updateRoleNameConstraint() {
        String checkQuery = """
            SELECT pg_get_constraintdef(c.oid)
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            WHERE t.relname = ? AND c.conname = ?
            """;

        List<String> defs = jdbcTemplate.query(checkQuery, (rs, rowNum) -> rs.getString(1), ROLE_TABLE_NAME, ROLE_CONSTRAINT_NAME);

        if (defs.isEmpty()) {
            log.info("[DatabaseConstraintInitializer] {} not present; skipping.", ROLE_CONSTRAINT_NAME);
            return;
        }

        String currentDef = defs.get(0);
        if (currentDef != null && currentDef.contains("PATIENT")) {
            log.info("[DatabaseConstraintInitializer] {} already permits PATIENT; no update needed.", ROLE_CONSTRAINT_NAME);
            return;
        }

        log.info("[DatabaseConstraintInitializer] Atomically updating {} to include PATIENT...", ROLE_CONSTRAINT_NAME);

        String atomicUpdateSql = """
            ALTER TABLE roles
                DROP CONSTRAINT IF EXISTS roles_name_check,
                ADD CONSTRAINT roles_name_check
                CHECK (name::text = ANY (ARRAY[
                    'PLATFORM_ADMIN'::character varying,
                    'HOSPITAL_ADMIN'::character varying,
                    'DOCTOR'::character varying,
                    'RECEPTIONIST'::character varying,
                    'LAB_STAFF'::character varying,
                    'PHARMACY_STAFF'::character varying,
                    'PATIENT'::character varying
                ]::text[]))
            """;

        jdbcTemplate.execute(atomicUpdateSql);
        log.info("[DatabaseConstraintInitializer] Successfully updated {} atomically.", ROLE_CONSTRAINT_NAME);
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProductName = metaData.getDatabaseProductName();
            if (dbProductName == null || !dbProductName.toLowerCase().contains("postgresql")) {
                log.info("[DatabaseConstraintInitializer] Non-PostgreSQL database ({}); skipping check constraint update.", dbProductName);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("[DatabaseConstraintInitializer] Could not determine database product name: {}", ex.getMessage());
            return false;
        }
    }
}
