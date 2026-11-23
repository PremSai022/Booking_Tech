package com.hyderabad.booking.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * MySQL DDL is not transactional, so a failed first migration leaves half-created tables and a
 * "failed" row in flyway_schema_history that blocks every later startup. If no migration has ever
 * succeeded (so the schema holds no app data), wipe the app's own schema and migrate again.
 */
@Configuration
public class FlywayRecoveryConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRecoveryConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            @Value("${app.flyway.auto-recover-failed-initial:true}") boolean autoRecover) {
        return flyway -> {
            if (autoRecover && hasOnlyFailedMigrations(flyway)) {
                log.warn("Previous initial Flyway migration failed and nothing was applied successfully; "
                        + "cleaning schema and re-running migrations.");
                Flyway.configure()
                        .configuration(flyway.getConfiguration())
                        .cleanDisabled(false)
                        .load()
                        .clean();
            }
            flyway.migrate();
        };
    }

    static boolean hasOnlyFailedMigrations(Flyway flyway) {
        MigrationInfo[] applied = flyway.info().applied();
        return applied.length > 0
                && Arrays.stream(applied).anyMatch(m -> m.getState().isFailed())
                && Arrays.stream(applied).noneMatch(m -> m.getState().isApplied() && !m.getState().isFailed()
                        && m.getVersion() != null && m.getType() != null && !m.getType().isBaseline());
    }
}
