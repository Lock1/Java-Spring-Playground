package com.brush.play;

import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.checkerframework.checker.tainting.qual.Untainted;
import org.h2.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.brush.play.sub.MyBatisMapperFacade;
import com.brush.play.sub.MyBatisMapperFacade.SqlResultHandler;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@EnableWebSecurity
public class PlayApplication {
    public sealed interface Maybe<T> {
        public record Has<T>(T value) implements Maybe<T> {}
        public record Empty<T>() implements Maybe<T> {}
    }

    // it looks like with 3.5.19, we can use standard xml stuff here as well
    // what about XML forloop?
    private static final @Untainted String SQL_STRING = """
        WITH MyInlineTable(Column1, Column2) AS (
            SELECT 1, 'Red'
            UNION ALL SELECT 2, 'Green'
            UNION ALL SELECT 3, 'Blue'
        )
        SELECT Column1, Column2
        FROM MyInlineTable
        WHERE Column1 > #{second};
    """;

    public static void main(String[] args) {
        SpringApplication.run(PlayApplication.class, args);
    }

    @SuppressWarnings("unused")
    @Bean
    RouterFunction<ServerResponse> routerFunction(MyBatisMapperFacade mapper) {
        record ReflectThis(
            String first,
            int second,
            long third,
            byte[] fourth,
            String fifth
        ) {}
        final SqlResultHandler<Map<String,Object>> transformer = map -> { log.info("here: {}", map); return map; };
        return RouterFunctions.route()
            .GET("/hello", e -> {
                return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.executeSelect(SQL_STRING, transformer));
            }).GET("/explode", __ -> {
                return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.executeSelect(SQL_STRING, transformer, new ReflectThis(null, 2, 1, new byte[1], null)));
            }).GET("/hard", __ -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.executeSelect((@Untainted String) "SELECT * FROM TEST", map -> map.get("ID")))
            ).build();
    }

    @RestController
    public static class Stub {
        @GetMapping("/root")
        public Object f(@RequestParam Optional<String> what) {
            log.info("here: |{}|", what);
            return what;
        }
    }

    @Bean
    @Order(1) // This is required
    public SecurityFilterChain http2(HttpSecurity http) throws Exception {
        final var a = http.securityMatcher("/root")
            .authorizeHttpRequests(e -> e.anyRequest().permitAll())
            .build();
        log.info("filter1 : |{}|", a.getFilters());
        return a;
    }

    @Bean
    @Order(9999)
    public SecurityFilterChain http(HttpSecurity http) throws Exception {
        final var a = http.authorizeHttpRequests(e -> e.anyRequest().denyAll())
            .build();
        log.info("filter2 : |{}|", a.getFilters());
        return a;
    }

    @Bean
    ServletWebServerFactory servletWebServerFactory() { return new TomcatServletWebServerFactory(1337); }

    @Bean
    DataSource dataSource() {
        org.apache.ibatis.logging.LogFactory.useStdOutLogging();
        final var a = new SimpleDriverDataSource(new Driver(), "jdbc:h2:~/test");
        a.setUsername("sa");
        a.setPassword("");
        return a;
    }
}

