package com.hyderabad.booking;

import com.hyderabad.booking.config.FlywayRecoveryConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayRecoveryConfigTest {

    private Flyway newFlyway(DriverManagerDataSource ds) {
        return Flyway.configure().dataSource(ds).locations("classpath:db/migration").load();
    }

    private DriverManagerDataSource newDataSource() {
        return new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
    }

    @Test
    void recoversFromFailedInitialMigration() {
        DriverManagerDataSource ds = newDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        newFlyway(ds).migrate();
        jdbc.update("UPDATE \"flyway_schema_history\" SET \"success\" = FALSE WHERE \"version\" = '1'");

        new FlywayRecoveryConfig().flywayMigrationStrategy(true).migrate(newFlyway(ds));

        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "SELECT \"success\" FROM \"flyway_schema_history\" WHERE \"version\" = '1'", Boolean.class));
    }

    @Test
    void keepsDataWhenMigrationsSucceeded() {
        DriverManagerDataSource ds = newDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        newFlyway(ds).migrate();
        jdbc.update("INSERT INTO users (phone, name, role) VALUES ('999', 'A', 'CUSTOMER')");

        new FlywayRecoveryConfig().flywayMigrationStrategy(true).migrate(newFlyway(ds));

        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class) == 1);
    }
}
