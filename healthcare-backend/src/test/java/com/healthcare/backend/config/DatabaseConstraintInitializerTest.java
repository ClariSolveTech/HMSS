package com.healthcare.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseConstraintInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    private DatabaseConstraintInitializer initializer;

    @BeforeEach
    void setUp() throws SQLException {
        initializer = new DatabaseConstraintInitializer(jdbcTemplate, dataSource);
    }

    @Test
    @DisplayName("Non-PostgreSQL database: skips execution cleanly without calling jdbcTemplate")
    void testNonPostgresDatabase_skipsExecution() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        initializer.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).execute(anyString());
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(), any());
    }

    @Test
    @DisplayName("Missing constraint: skips update and releases advisory lock")
    void testMissingConstraint_skipsUpdate() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(DatabaseConstraintInitializer.TABLE_NAME), eq(DatabaseConstraintInitializer.CONSTRAINT_NAME)))
                .thenReturn(Collections.emptyList());

        initializer.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).execute("SELECT pg_advisory_lock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")");
        verify(jdbcTemplate).execute("SELECT pg_advisory_unlock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")");
        verify(jdbcTemplate, never()).execute(contains("ALTER TABLE"));
    }

    @Test
    @DisplayName("Already correct constraint (contains TELECONSULTATION): skips update and releases advisory lock")
    void testAlreadyCorrectConstraint_skipsUpdate() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        String validDef = "CHECK (appointment_type::text = ANY (ARRAY['NEW_CONSULTATION'::text, 'FOLLOW_UP'::text, 'EMERGENCY'::text, 'WALK_IN'::text, 'TELECONSULTATION'::text]))";
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(DatabaseConstraintInitializer.TABLE_NAME), eq(DatabaseConstraintInitializer.CONSTRAINT_NAME)))
                .thenReturn(List.of(validDef));

        initializer.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).execute("SELECT pg_advisory_lock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")");
        verify(jdbcTemplate).execute("SELECT pg_advisory_unlock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")");
        verify(jdbcTemplate, never()).execute(contains("ALTER TABLE"));
    }

    @Test
    @DisplayName("Outdated constraint (missing TELECONSULTATION): executes atomic ALTER TABLE and releases advisory lock")
    void testOutdatedConstraint_executesAtomicUpdate() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        String outdatedDef = "CHECK (appointment_type::text = ANY (ARRAY['NEW_CONSULTATION'::text, 'FOLLOW_UP'::text, 'EMERGENCY'::text, 'WALK_IN'::text]))";
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(DatabaseConstraintInitializer.TABLE_NAME), eq(DatabaseConstraintInitializer.CONSTRAINT_NAME)))
                .thenReturn(List.of(outdatedDef));

        initializer.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).execute("SELECT pg_advisory_lock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(3)).execute(sqlCaptor.capture());

        List<String> executedStatements = sqlCaptor.getAllValues();
        assertEquals("SELECT pg_advisory_lock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")", executedStatements.get(0));

        String alterSql = executedStatements.get(1);
        assertTrue(alterSql.contains("DROP CONSTRAINT IF EXISTS appointments_appointment_type_check"));
        assertTrue(alterSql.contains("ADD CONSTRAINT appointments_appointment_type_check"));
        assertTrue(alterSql.contains("'TELECONSULTATION'::character varying"));
        assertTrue(alterSql.contains("'NEW_CONSULTATION'::character varying"));
        assertTrue(alterSql.contains("'FOLLOW_UP'::character varying"));
        assertTrue(alterSql.contains("'EMERGENCY'::character varying"));
        assertTrue(alterSql.contains("'WALK_IN'::character varying"));

        assertEquals("SELECT pg_advisory_unlock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")", executedStatements.get(2));
    }

    @Test
    @DisplayName("Update failure (e.g. database error): catches exception safely, logs error, and still releases advisory lock")
    void testUpdateFailure_handlesExceptionSafelyAndReleasesLock() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        String outdatedDef = "CHECK (appointment_type::text = ANY (ARRAY['NEW_CONSULTATION'::text]))";
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(DatabaseConstraintInitializer.TABLE_NAME), eq(DatabaseConstraintInitializer.CONSTRAINT_NAME)))
                .thenReturn(List.of(outdatedDef));

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("ALTER TABLE")) {
                throw new DataAccessResourceFailureException("Database permission denied");
            }
            return null;
        }).when(jdbcTemplate).execute(anyString());

        assertDoesNotThrow(() -> initializer.run(new DefaultApplicationArguments()));

        verify(jdbcTemplate).execute("SELECT pg_advisory_lock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")");
        verify(jdbcTemplate).execute("SELECT pg_advisory_unlock(" + DatabaseConstraintInitializer.ADVISORY_LOCK_ID + ")");
    }

    @Test
    @DisplayName("Outdated role constraint (missing PATIENT): executes atomic ALTER TABLE for roles")
    void testOutdatedRoleConstraint_executesAtomicUpdate() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        String currentRoleDef = "CHECK (name::text = ANY (ARRAY['DOCTOR'::text, 'RECEPTIONIST'::text]))";
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(DatabaseConstraintInitializer.TABLE_NAME), eq(DatabaseConstraintInitializer.CONSTRAINT_NAME)))
                .thenReturn(Collections.emptyList());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(DatabaseConstraintInitializer.ROLE_TABLE_NAME), eq(DatabaseConstraintInitializer.ROLE_CONSTRAINT_NAME)))
                .thenReturn(List.of(currentRoleDef));

        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeast(2)).execute(sqlCaptor.capture());

        List<String> executed = sqlCaptor.getAllValues();
        boolean foundRoleAlter = executed.stream().anyMatch(sql ->
                sql.contains("DROP CONSTRAINT IF EXISTS roles_name_check") &&
                sql.contains("ADD CONSTRAINT roles_name_check") &&
                sql.contains("'PATIENT'::character varying")
        );
        assertTrue(foundRoleAlter, "Expected atomic ALTER TABLE statement for roles_name_check containing PATIENT");
    }
}
