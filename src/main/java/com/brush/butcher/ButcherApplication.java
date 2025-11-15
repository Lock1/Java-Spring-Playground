package com.brush.butcher;

import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

@SpringBootApplication
public class ButcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(ButcherApplication.class, args);
    }

    @Bean
    DataSource dataSource() {
        final var dataSource = new SimpleDriverDataSource(new Driver(), "jdbc:h2:mem:db");
        dataSource.setUsername("sa");
        dataSource.setPassword("password");
        return dataSource;
    }
}
