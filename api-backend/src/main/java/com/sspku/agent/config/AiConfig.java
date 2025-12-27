package com.sspku.agent.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public RestClient.Builder restClientBuilderProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMinutes(5));
        factory.setReadTimeout(Duration.ofMinutes(5));
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public WebClient.Builder webClientBuilderProvider() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofMinutes(5))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300_000)
                                .doOnConnected(conn ->
                                        conn.addHandlerLast(new ReadTimeoutHandler(300))
                                                .addHandlerLast(new WriteTimeoutHandler(300)))
                ));
    }
}
