package com.brush.play;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableEncryptableProperties
@Slf4j
public class PlayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlayApplication.class, args);
        // RouterFunction<>
    }

    @Bean
    ServletWebServerFactory servletWebServerFactory() {
        return new TomcatServletWebServerFactory(1337);
    }

    @EventListener(classes=WebServerInitializedEvent.class)
    void a(WebServerInitializedEvent event) {
        System.out.println("What is this?");
        System.out.println(event.getTimestamp());
    }

    @FunctionalInterface
    interface BuilderInterface<Builder,Result> {
        Result construct(Consumer<Builder> configurator);
    }

    public static <Builder,Result> BuilderInterface<Builder,Result> functionalBuilder(
        Supplier<? extends Builder> builder,
        Function<Builder,? extends Result> finalizer
    ) {
        return configurator -> {
            final Builder builderInstance = builder.get();
            configurator.accept(builderInstance);
            return finalizer.apply(builderInstance);
        };
    }

    @Bean
    DataSource dataSource() {
        return functionalBuilder(() -> new SimpleDriverDataSource(new Driver(), "jdbc:h2:mem:db"), x -> x)
            .construct(dataSource -> {
                dataSource.setUsername("sa");
                dataSource.setPassword("password");
            });
    }
}
