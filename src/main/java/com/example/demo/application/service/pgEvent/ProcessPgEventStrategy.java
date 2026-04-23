package com.example.demo.application.service.pgEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.PgEventType;
import com.example.demo.domain.service.ProcessPgEventService;

@Component
public class ProcessPgEventStrategy {

    private final Map<String, ProcessPgEventService> handlers = new ConcurrentHashMap<>();

    public ProcessPgEventStrategy(ApplicationContext context) {
        Map<String, Object> beans = context.getBeansWithAnnotation(PgEventType.class);

        beans.forEach((name, bean) -> {
            PgEventType annotation = bean.getClass().getAnnotation(PgEventType.class);
            handlers.put(annotation.value(), (ProcessPgEventService) bean);
        });
    }

    public ProcessPgEventService getHandler(String datatype) {
        return handlers.get(datatype);
    }
}