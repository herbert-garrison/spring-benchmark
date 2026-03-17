package com.benchmark.webmvc.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class VirtualThreadConfig {
    public static final String TASK_EXECUTOR_BEAN = "taskExecutor";

    // @Async methods are using VT as well
    @Bean(TASK_EXECUTOR_BEAN)
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
