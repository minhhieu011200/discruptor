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
        processMarketReceiveService.process("312321");
        // try {
        // URI uri = new
        // URI("wss://streaming.forexpros.com/echo/866/dnaodnzt/websocket");
        // client = new WebSocketClient(uri) {
        // @Override
        // public void onOpen(ServerHandshake handshake) {
        // System.out.println("Connected to ForexPros WebSocket!");

        // // gửi bulk-subscribe message
        // String subscribeMessage =
        // "[\"{\\\"_event\\\":\\\"bulk-subscribe\\\",\\\"tzID\\\":8,\\\"message\\\":\\\"isOpenExch-NaN:%%pid-958739:%%isOpenExch-122:%%pid-958482:%%pid-958514:%%pid-1072434:%%pid-958674:%%pid-1009015:%%pid-958561:%%pid-953722:%%pid-958731:%%pid-1072044:%%isOpenExch-72:%%pid-958621:%%pid-958570:%%pid-958483:%%pid-958459:%%pid-1043364:%%pid-41064:%%pidExt-41064:%%pid-995068:%%pidExt-995068:%%pid-995072:%%pidExt-995072:%%pid-1175153:%%isOpenExch-152:%%pidExt-1175153:%%pid-169:%%isOpenExch-1:%%pidExt-169:%%pid-179:%%isOpenExch-21:%%pidExt-179:%%pid-27:%%isOpenExch-3:%%pidExt-27:%%pid-6408:%%isOpenExch-2:%%pid-6369:%%pid-243:%%pid-267:%%pid-7888:%%pid-284:%%pid-352:%%isOpenExch-4:%%pidExt-958731:%%pidExt-958621:%%pid-1076910:%%pidExt-1076910:%%pid-958751:%%pidExt-958751:%%pidExt-958482:%%pid-41913:%%pidExt-41913:%%pid-42141:%%pidExt-42141:%%pid-2214:%%isOpenExch-1002:%%pidExt-2214:%%pid-1:%%pidExt-1:%%pid-2:%%pidExt-2:%%pid-3:%%pidExt-3:%%pid-5:%%pidExt-5:%%pid-4:%%pidExt-4:%%pid-11:%%pidExt-11:%%pid-8830:%%isOpenExch-1004:%%pid-8836:%%pid-8831:%%pid-8849:%%pid-8833:%%pid-8862:%%pid-8832:%%pid-20:%%pid-166:%%pid-172:%%pid-167:%%isOpenExch-9:%%pid-178:%%isOpenExch-20:%%pid-7:%%pid-9:%%pid-10:%%pidExt-8830:%%pidExt-8849:%%pidExt-8862:%%pidExt-8836:%%pid-49771:%%pidExt-49771:%%pid-49773:%%isOpenExch-93:%%pidExt-49773:%%pid-13916:%%pidExt-13916:%%pid-1131557:%%pid-1234340:%%pid-958113:%%pid-68:%%pid-9019:%%pid-345:%%pid-6290:%%pid-6710:%%pid-6902:%%pid-13831:%%pid-14145:%%pid-44480:%%pid-986242:%%pid-1202148:%%pid-7857:%%pid-16638:%%pid-21218:%%pid-18:%%pidExt-18:%%pidExt-7:%%pidExt-9:%%pid-8:%%pidExt-8:%%pid-6:%%pidExt-6:%%pidExt-10:%%pid-49:%%pidExt-49:%%pid-13:%%pidExt-13:%%pid-16:%%pidExt-16:%%pid-47:%%pidExt-47:%%pid-51:%%pidExt-51:%%pid-58:%%pidExt-58:%%pid-50:%%pidExt-50:%%pid-53:%%pidExt-53:%%pid-15:%%pidExt-15:%%pid-12:%%pidExt-12:%%pid-52:%%pidExt-52:%%pid-48:%%pidExt-48:%%pid-55:%%pidExt-55:%%pid-160:%%pidExt-160:%%pid-2111:%%pidExt-2111:%%isOpenExch-1001:%%pid-42:%%pidExt-42:%%pid-155:%%pidExt-155:%%pid-43:%%pidExt-43:%%pid-54:%%pidExt-54:%%pid-41:%%pidExt-41:%%pid-2186:%%pidExt-2186:%%pid-63:%%pidExt-63:%%pid-39:%%pidExt-39:%%pid-17:%%pidExt-17:%%pid-14:%%pidExt-14:%%pid-56:%%pidExt-56:%%pid-57:%%pidExt-57:%%pid-945629:%%pidExt-945629:%%isOpenExch-1014:%%pid-49800:%%pidExt-49800:%%isOpenExch-1008:%%pid-1058142:%%pidExt-1058142:%%isOpenExch-1027:%%pid-42100:%%pidExt-42100:%%isOpenExch-0:%%pid-42135:%%pidExt-42135:%%pid-958421:%%pidExt-958421:%%pid-958444:%%pidExt-958444:%%pid-958472:%%pidExt-958472:%%pid-958540:%%pidExt-958540:%%pid-958572:%%pidExt-958572:%%pid-958586:%%pidExt-958586:%%pid-958597:%%pidExt-958597:%%pid-958629:%%pidExt-958629:%%pid-41942:%%pidExt-41942:%%pid-42013:%%pidExt-42013:%%pid-42031:%%pidExt-42031:%%pidExt-953722:%%pid-958386:%%pidExt-958386:%%pid-958402:%%pidExt-958402:%%pid-958438:%%pidExt-958438:%%pid-958462:%%pidExt-958462:%%pid-958481:%%pidExt-958481:%%pid-958520:%%pidExt-958520:\\\"}\"]";
        // client.send(subscribeMessage);
        // }

        // @Override
        // public void onMessage(String message) {

        // processMarketReceiveService.process(message);
        // }

        // @Override
        // public void onClose(int code, String reason, boolean remote) {
        // System.out.println("Disconnected: " + reason);
        // this.reconnect();
        // }

        // @Override
        // public void onError(Exception ex) {
        // ex.printStackTrace();
        // }
        // };
        // client.connect();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }
}
