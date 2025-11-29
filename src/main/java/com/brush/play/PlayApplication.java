package com.brush.play;

import java.util.Map;

import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.brush.play.MyBatisMapperShim.SqlResultHandler;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class PlayApplication {
    // it looks like with 3.5.19, we can use standard xml stuff here as well
    // what about XML forloop?
    private static final String SQL_STRING = """
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
    RouterFunction<ServerResponse> routerFunction(MyBatisMapperShim mapper) {
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
                .body(mapper.executeSelect("SELECT * FROM TEST", map -> map.get("ID")))
            ).build();
    }

    @Bean
    ServletWebServerFactory servletWebServerFactory() { return new TomcatServletWebServerFactory(1337); }

    @Bean
    DataSource dataSource() {
        final var a = new SimpleDriverDataSource(new Driver(), "jdbc:h2:~/test");
        a.setUsername("sa");
        a.setPassword("");
        return a;
    }
}

