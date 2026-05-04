package com.example.demo.application.service.pgEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.PgEventType;
import com.example.demo.domain.service.ProcessPgEventService;

@Component
public class ProcessPgEventStrategy implements SmartInitializingSingleton {

    private final Map<String, ProcessPgEventService> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext context;

    public ProcessPgEventStrategy(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beans = context.getBeansWithAnnotation(PgEventType.class);

        beans.forEach((name, bean) -> {
            PgEventType annotation = AnnotationUtils.findAnnotation(bean.getClass(), PgEventType.class);
            if (annotation != null) {
                handlers.put(annotation.value(), (ProcessPgEventService) bean);
            }
        });
    }

    public ProcessPgEventService getHandler(String datatype) {
        return handlers.get(datatype);
    }
}