package ru.practicum.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"ru.practicum.event", "ru.practicum.stats"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"ru.practicum.category.client", "ru.practicum.user.client", "ru.practicum.request.client"})
public class EventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}