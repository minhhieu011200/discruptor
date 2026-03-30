package com.example.demo.presentation.controller;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import com.example.demo.application.service.ProcessMarketReceiveSocket;
import com.example.demo.domain.service.ProcessMarketReceiveService;

import jakarta.annotation.PostConstruct;

@Component
public class ReceivedMarketSocket {
    private WebSocketClient client;
    private final ProcessMarketReceiveSocket processMarketReceiveService;

    private ReceivedMarketSocket(ProcessMarketReceiveSocket processMarketReceiveService) {
        this.processMarketReceiveService = processMarketReceiveService;
    }

    @PostConstruct
    public void init() {
        System.out.println("Connected to ForexPros WebSocket!");
        connectAndSubscribe(); // 🚀 chạy ngay khi bean được tạo
    }

    public void connectAndSubscribe() {
        // processMarketReceiveService.process("312321");
        try {
            URI uri = new URI("wss://streaming.forexpros.com/echo/866/dnaodnzt/websocket");
            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected to ForexPros WebSocket!");

                    // gửi bulk-subscribe message
                    String subscribeMessage = "[\"{\\\"_event\\\":\\\"bulk-subscribe\\\",\\\"tzID\\\":110,\\\"message\\\":\\\"isOpenExch-1002:%%pid-2214:%%isOpenPair-1:%%pid-1:%%isOpenPair-2:%%pid-2:%%isOpenPair-3:%%pid-3:%%isOpenPair-5:%%pid-5:%%isOpenPair-4:%%pid-4:%%isOpenExch-1002:%%pid-11:\\\"}\"]";
                    client.send(subscribeMessage);
                }

                @Override
                public void onMessage(String message) {

                    processMarketReceiveService.process(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected: " + reason);
                    this.reconnect();
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
