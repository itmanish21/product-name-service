package com.jaeger.poc;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@SpringBootApplication
public class ProductNameService {

    public static void main(String[] args) {
        SpringApplication.run(ProductNameService.class, args);
    }

    @Bean
    public Tracer tracer() {
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
                .withLogSpans(true);

        Configuration config = new Configuration("product-svc")
                .withSampler(samplerConfig)
                .withReporter(reporterConfig);

        return config.getTracer();
    }
}

@RestController
@RequestMapping("/api/v1/products")
class AnimalNameResource {

    private final List<String> productNames;
    private Random random;
    @Autowired
    private Tracer tracer;


    public AnimalNameResource() throws IOException {
        InputStream inputStream = new ClassPathResource("/products.txt").getInputStream();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
        	productNames = reader.lines().collect(Collectors.toList());
        }
        random = new Random();
    }

    @GetMapping(path = "/random")
    public String name(@RequestHeader HttpHeaders headers) {
        SpanContext parentContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers.toSingleValueMap()));
        Span span = tracer.buildSpan("find-random-product-name").asChildOf(parentContext).start();
        String name = productNames.get(random.nextInt(productNames.size()));
        span.finish();
        return name;
    }
}

