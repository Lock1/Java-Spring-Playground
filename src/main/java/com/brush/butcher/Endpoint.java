package com.brush.butcher;

import java.time.Duration;
import java.util.Random;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;



@RestController
public class Endpoint {
    @GetMapping("/hello")
    Object reflectThis() {
        record R(String a, int b, String c) {}
        return new R("abcdef", 10, "1337");
    }

    @GetMapping("/dev/random")
    ResponseEntity<Flux<String>> streaming() {
        final var random = new Random(1337);
        final var flux = Flux.fromStream(random.ints().limit(10).mapToObj(Integer::toString))
            .delayElements(Duration.ofSeconds(1));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "atttachment; filename=abc.csv")
            .body(flux);
    }
}
