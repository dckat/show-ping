package com.ssginc.showpingrefactoring.common.config;

import com.ssginc.showpingrefactoring.common.handler.LiveHandler;
import com.ssginc.showpingrefactoring.common.handler.RecordHandler;
import com.ssginc.showpingrefactoring.common.util.UserRegistry;
import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;


/**
 * @author dckat
 * 웹소켓 설정 클래스
 * <p>
 */
@Configuration
@EnableWebSocket
@Profile("!test")
public class WebSocketConfig implements WebSocketConfigurer {

    @Bean
    public LiveHandler liveHandler() {
        return new LiveHandler();
    }

    @Bean
    public RecordHandler recordHandler() {
        return new RecordHandler();
    }

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create();
    }

    @Bean
    public UserRegistry registry() {
        return new UserRegistry();
    }

    @Bean
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(32768);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveHandler(), "/live")
                .setAllowedOrigins("*");
//                .setAllowedOrigins("https://showping.duckdns.org");
        registry.addHandler(recordHandler(), "/record")
                .setAllowedOrigins("*");
//                .setAllowedOrigins("https://showping.duckdns.org");
    }

}
