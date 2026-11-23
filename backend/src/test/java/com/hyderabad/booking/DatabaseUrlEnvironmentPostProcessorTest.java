package com.hyderabad.booking;

import com.hyderabad.booking.config.DatabaseUrlEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsRailwayMysqlUrl() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "mysql://root:p%40ss@mysql.railway.internal:3306/railway")
                .withProperty("spring.datasource.username", "wrong");

        processor.postProcessEnvironment(env, null);

        assertEquals("jdbc:mysql://mysql.railway.internal:3306/booking_db?useSSL=false&serverTimezone=UTC"
                        + "&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true",
                env.getProperty("spring.datasource.url"));
        assertEquals("root", env.getProperty("spring.datasource.username"));
        assertEquals("p@ss", env.getProperty("spring.datasource.password"));
    }

    @Test
    void forcesOwnDatabaseOnJdbcUrl() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/railway");

        processor.postProcessEnvironment(env, null);

        assertEquals("jdbc:mysql://localhost:3306/booking_db?useSSL=false&serverTimezone=UTC"
                        + "&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true", env.getProperty("spring.datasource.url"));
        assertNull(env.getProperty("spring.datasource.password"));
    }

    @Test
    void usesDbNameOverride() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://h:3306/railway?useSSL=true")
                .withProperty("DB_NAME", "my_app");

        processor.postProcessEnvironment(env, null);

        assertEquals("jdbc:mysql://h:3306/my_app?useSSL=true&createDatabaseIfNotExist=true",
                env.getProperty("spring.datasource.url"));
    }
}
