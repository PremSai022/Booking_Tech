package com.hyderabad.booking.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes MySQL datasource URLs:
 * <ul>
 *   <li>Converts provider-style URLs (Railway's {@code mysql://user:pass@host:port/db}) into a JDBC URL
 *   plus username/password, since Hikari only accepts {@code jdbc:} URLs.</li>
 *   <li>Forces the app's own database ({@code DB_NAME}, default {@code booking_db}) so it never shares
 *   the provider's default schema (e.g. {@code railway}) with other apps, and creates it if missing.</li>
 * </ul>
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";
    static final String DEFAULT_DATABASE = "booking_db";
    private static final String CREATE_DB_PARAM = "createDatabaseIfNotExist=true";
    private static final String DEFAULT_PARAMS =
            "serverTimezone=UTC&allowPublicKeyRetrieval=true&" + CREATE_DB_PARAM;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null) {
            return;
        }
        String trimmed = url.trim();
        boolean providerUrl = trimmed.startsWith("mysql://");
        if (!providerUrl && !trimmed.startsWith("jdbc:mysql://")) {
            return;
        }

        URI uri = URI.create(providerUrl ? trimmed : trimmed.substring("jdbc:".length()));
        Map<String, Object> props = new HashMap<>();

        String database = environment.getProperty("DB_NAME", DEFAULT_DATABASE).trim();
        if (database.isEmpty()) {
            database = DEFAULT_DATABASE;
        }

        StringBuilder jdbc = new StringBuilder("jdbc:mysql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append('/').append(database);

        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            query = "useSSL=" + environment.getProperty("DB_USE_SSL", "false") + "&" + DEFAULT_PARAMS;
        } else if (!query.contains("createDatabaseIfNotExist")) {
            query = query + "&" + CREATE_DB_PARAM;
        }
        jdbc.append('?').append(query);
        props.put("spring.datasource.url", jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (providerUrl && userInfo != null && !userInfo.isEmpty()) {
            int idx = userInfo.indexOf(':');
            String user = idx >= 0 ? userInfo.substring(0, idx) : userInfo;
            props.put("spring.datasource.username", decode(user));
            if (idx >= 0) {
                props.put("spring.datasource.password", decode(userInfo.substring(idx + 1)));
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
