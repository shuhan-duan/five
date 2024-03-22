package com.five.config;

import jakarta.websocket.server.ServerEndpointConfig;


public class CustomSpringConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return ApplicationContextProvider.getApplicationContext().getBean(clazz);
    }
}
