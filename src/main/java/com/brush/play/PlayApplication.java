package com.brush.play;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.checkerframework.checker.tainting.qual.Untainted;
import org.h2.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
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
    // what about XML OGNL-Batis forloop?
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

    // @SuppressWarnings("unused")
    // @Bean
    // RouterFunction<ServerResponse> routerFunction(MyBatisMapperFacade mapper) {
    //     record ReflectThis(
    //         String first,
    //         int second,
    //         long third,
    //         byte[] fourth,
    //         String fifth
    //     ) {}
    //     final SqlResultHandler<Map<String,Object>> transformer = map -> { log.info("here: {}", map); return map; };
    //     return RouterFunctions.route()
    //         .GET("/hello", e -> {
    //             return ServerResponse.ok()
    //                 .contentType(MediaType.APPLICATION_JSON)
    //                 .body(mapper.executeSelect(SQL_STRING, transformer));
    //         }).GET("/explode", __ -> {
    //             return ServerResponse.ok()
    //                 .contentType(MediaType.APPLICATION_JSON)
    //                 .body(mapper.executeSelect(SQL_STRING, transformer, new ReflectThis(null, 2, 1, new byte[1], null)));
    //         }).GET("/hard", __ -> ServerResponse.ok()
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .body(mapper.executeSelect((@Untainted String) "SELECT * FROM TEST", map -> map.get("ID")))
    //         ).build();
    // }

    @Service
    public static class SystemIOService {
        public final RandomService random = new RandomService();

        public long fetchSystemEpochMillis() {
            return System.currentTimeMillis();
        }

        public static class RandomService {
            private static final ThreadLocal<SecureRandom> THREAD_LOCAL_CSPRNG = ThreadLocal.withInitial(SecureRandom::new);

            public byte[] generateBytes(int length) {
                final var buffer = new byte[length];
                THREAD_LOCAL_CSPRNG.get().nextBytes(buffer);
                return buffer;
            }

            public IntStream generateInts() {
                return THREAD_LOCAL_CSPRNG.get().ints();
            }

            private enum ASCIILookupTable { ;
                static final char[] ALPHANUMERIC = new char[] {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                    'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D',
                    'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                    'Y', 'Z',
                };
            }

            public String generateAlphanumericString(int length) {
                return THREAD_LOCAL_CSPRNG.get().ints(0, ASCIILookupTable.ALPHANUMERIC.length) // Looks like SecureRandom provide convenient rejection sampling API
                    .limit(length)
                    .collect(
                        StringBuffer::new,
                        (acc, i) -> { acc.append(ASCIILookupTable.ALPHANUMERIC[i]); },
                        (_, _) -> { throw new RuntimeException("Buggy code: Unexpected parallel stream"); }
                    ).toString();
            }
        }
    }

    @RestController
    @AllArgsConstructor
    public static class Stub {
        private final SystemIOService io;

        @GetMapping("/root")
        public Object f(@RequestParam Optional<String> what) {
            log.info("here: |{}|", what);
            return what;
        }

        @GetMapping("/free-cookie")
        public void giveMeCookie(HttpServletResponse response) {
            response.addCookie(new Cookie("this-is-not-a-cookie", "ceci-nest-pas-une-pipe"));
        }

        @GetMapping("/dev/urandom")
        public String whoa() {
            return io.random.generateAlphanumericString(10);
        }
    }

    @Bean
    // @Order(1) // This is required
    public SecurityFilterChain http2(HttpSecurity http) throws Exception {
        final var a = http
            .formLogin(x -> {})
            .securityMatcher("/**")
            // .securityMatcher("/free-cookie", "/nuke")
            // .authorizeHttpRequests(e -> e.anyRequest().permitAll())
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/nuke"))
                .logoutSuccessHandler((req, res, __) -> {
                    log.info("{}", req);
                    res.sendRedirect("/root");
                })
                .deleteCookies("this-is-not-a-cookie")
            ).build();
        log.info("filter1 : |{}|", a.getFilters());
        return a;
    }

    // @Bean
    // @Order(9999)
    // public SecurityFilterChain http(HttpSecurity http) throws Exception {
    //     final var a = http.authorizeHttpRequests(e -> e.anyRequest().denyAll())
    //         .build();
    //     log.info("filter2 : |{}|", a.getFilters());
    //     return a;
    // }

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

