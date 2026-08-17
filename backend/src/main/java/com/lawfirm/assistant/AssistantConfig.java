package com.lawfirm.assistant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 助手相关 Bean 配置
 */
@Configuration
public class AssistantConfig {

    /** 用于 SSE 流式对话的异步执行线程池 */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService assistantExecutor() {
        AtomicInteger n = new AtomicInteger(1);
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "assistant-" + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
        return Executors.newCachedThreadPool(tf);
    }
}
