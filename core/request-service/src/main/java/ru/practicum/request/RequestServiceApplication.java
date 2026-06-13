package ru.practicum.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"ru.practicum.request", "ru.practicum.event.client", "ru.practicum.user.client", "ru.practicum.exception"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"ru.practicum.event.client", "ru.practicum.user.client"})
public class RequestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApplication.class, args);
    }
}