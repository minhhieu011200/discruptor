package com.example.demo.infrastructure.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.domain.repository.EventPGQueueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PgEventRoute extends RouteBuilder {

    @Value("${pg.endpoint}")
    private String endpoint;

    private final CamelContext camelContext;
    private final EventPGQueueRepository eventQueue;
    private final ObjectMapper mapper;

    public PgEventRoute(CamelContext camelContext, EventPGQueueRepository eventQueue, ObjectMapper mapper) {
        this.camelContext = camelContext;
        this.eventQueue = eventQueue;
        this.mapper = mapper;
    }

    @Override
    public void configure() throws Exception {

        // Global error handler – retry vô hạn
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(-1)
                .redeliveryDelay(5000)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        // Khi consumer fail → restart nguyên route sau 5s
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "❌ PgEvent error: ${exception.message}")
                .delay(5000)
                .process(e -> {
                    String routeId = "pg-listener";
                    log.warn("🔁 Restarting PgEvent route: {}", routeId);

                    // Stop → Start lại route
                    try {
                        camelContext.getRouteController().stopRoute(routeId);
                    } catch (Exception ex) {
                        // ignore
                    }
                    camelContext.getRouteController().startRoute(routeId);
                });

        from(endpoint)
                .routeId("pg-listener")
                .autoStartup(true)
                .log("Received PG event: ${body}")
                .process(e -> {
                    String body = e.getIn().getBody(String.class);
                    JsonNode root = mapper.readTree(body);
                    String datatype = root.path("datatype").asText();
                    eventQueue.offer(datatype, root);
                });
    }
}