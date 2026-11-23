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
 * Converts provider-style URLs such as Railway's {@code mysql://user:pass@host:port/db}
 * into a JDBC URL plus username/password, since Hikari only accepts {@code jdbc:} URLs.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";
    private static final String DEFAULT_PARAMS =
            "serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null) {
            return;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("mysql://")) {
            return;
        }

        URI uri = URI.create(trimmed);
        Map<String, Object> props = new HashMap<>();

        StringBuilder jdbc = new StringBuilder("jdbc:mysql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getRawPath() == null ? "" : uri.getRawPath());
        String useSsl = environment.getProperty("DB_USE_SSL", "false");
        String query = uri.getRawQuery();
        jdbc.append('?').append(query != null && !query.isBlank()
                ? query
                : "useSSL=" + useSsl + "&" + DEFAULT_PARAMS);
        props.put("spring.datasource.url", jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
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
