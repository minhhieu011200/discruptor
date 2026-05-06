package com.example.demo.application.service;

import com.example.demo.application.annotation.Measured;
import com.example.demo.application.annotation.TraceLog;
import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.SymbolQueueRepository;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.domain.service.ProcessMarketEventService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProcessMarketEvent implements ProcessMarketEventService {
    private final SymbolRepository symbolRepository;
    private final TranslogShardedQueueRepository translogShardedQueueRepository;
    private final SymbolQueueRepository symbolQueueRepository;

    public ProcessMarketEvent(
            SymbolRepository symbolRepository,
            TranslogShardedQueueRepository translogShardedQueueRepository,
            SymbolQueueRepository symbolQueueRepository) {
        this.symbolRepository = symbolRepository;
        this.translogShardedQueueRepository = translogShardedQueueRepository;
        this.symbolQueueRepository = symbolQueueRepository;
        // this.marketDisruptor = marketDisruptor;
    }

    @Override
    @Measured(value = "disruptor.event.process", description = "Time to process a market event in disruptor")
    @TraceLog("ProcessMarketEvent")
    public void process(SymbolRequestDTO data) {

        String imt = data.getImtcode();
        if (imt == null || imt.length() == 0)
            return;

        // Hot-path: USDVND (luồng chạy nhiều)
        if (imt.equals("USDVND")) {
            processUsdVndFast(data);
            return;
        }

        // FX-USD (EURUSD, GBPUSD...)
        processFxUsdFast(data, imt);
    }

    // ============================================================
    // = HANDLE USDVND =
    // ============================================================

    private void processUsdVndFast(SymbolRequestDTO data) {
        SymbolEntity usdVnd = symbolRepository.get("USDVND");
        if (usdVnd == null)
            return;

        usdVnd.setBuyCurrency(data.getBuyCurrency());
        usdVnd.setSellCurrency(data.getSellCurrency());
        usdVnd.setBid(data.getBid());
        usdVnd.setAsk(data.getAsk());
        usdVnd.setTenor(data.getTenor());
        usdVnd.setStatus(data.getStatus());
        usdVnd.setImtCode();
        usdVnd.setValidFrom(data.getValidFrom());
        usdVnd.setValidTill(data.getValidTill());

        usdVnd.setVersion(usdVnd.getVersion() + 1);

        // MUST PUT
        symbolRepository.set("USDVND", usdVnd);

        fastTranslog(usdVnd);
    }

    private void processFxUsdFast(SymbolRequestDTO data, String imt) {

        SymbolEntity fxUsd = symbolRepository.get(imt);
        if (fxUsd == null)
            return;

        // Update data
        fxUsd.setBuyCurrency(data.getBuyCurrency());
        fxUsd.setSellCurrency(data.getSellCurrency());
        fxUsd.setBid(data.getBid());
        fxUsd.setAsk(data.getAsk());
        fxUsd.setTenor(data.getTenor());
        fxUsd.setStatus(data.getStatus());
        fxUsd.setImtCode();
        fxUsd.setValidFrom(data.getValidFrom());
        fxUsd.setValidTill(data.getValidTill());
        fxUsd.setRateQuoteID(data.getRateQuoteID());

        // Increment version
        fxUsd.setVersion(fxUsd.getVersion() + 1);

        // MUST: update ChronicleMap!
        symbolRepository.set(imt, fxUsd);

        fastTranslog(fxUsd);

        // =================================================
        // If not USD related → return early
        // =================================================
        boolean buyIsUsd = "USD".equals(data.getSellCurrency());
        boolean sellIsUsd = "USD".equals(data.getBuyCurrency());
        if (!buyIsUsd && !sellIsUsd)
            return;

        SymbolEntity usdVnd = symbolRepository.get("USDVND");
        if (usdVnd == null)
            return;

        // avoid string concat
        String crossImt = buyIsUsd ? data.getBuyCurrency() + "VND" : "VND" + data.getSellCurrency();
        SymbolEntity fxVnd = symbolRepository.get(crossImt);

        if (fxVnd == null) {
            fxVnd = new SymbolEntity();
        }

        if (buyIsUsd) {
            fxVnd.setBid(data.getBid() * usdVnd.getBid());
            fxVnd.setAsk(data.getAsk() * usdVnd.getAsk());
            fxVnd.setBuyCurrency(data.getBuyCurrency());
        } else {
            fxVnd.setBid(usdVnd.getAsk() / data.getBid());
            fxVnd.setAsk(usdVnd.getBid() / data.getAsk());
            fxVnd.setBuyCurrency(data.getSellCurrency());
        }

        fxVnd.setSellCurrency("VND");
        fxVnd.setTenor(data.getTenor());
        fxVnd.setImtCode();
        fxVnd.setValidFrom(data.getValidFrom());
        fxVnd.setValidTill(data.getValidTill());
        fxVnd.setRateQuoteID(data.getRateQuoteID());
        fxVnd.setVersion(fxVnd.getVersion() + 1);

        // MUST PUT BACK
        symbolRepository.set(crossImt, fxVnd);

        fastTranslog(fxVnd);
    }

    private void fastTranslog(SymbolEntity s) {
        TranslogEntity t = new TranslogEntity();

        // Ghi vào object slot riêng biệt
        t.hydrate(
                s.getImtcode(),
                s.getBuyCurrency(), s.getSellCurrency(),
                s.getTenor(),
                s.getBid(), s.getAsk(),
                s.getSpread(),
                s.getValidFrom(), s.getValidTill(),
                s.getRateQuoteID(),
                s.getVersion());

        translogShardedQueueRepository.offer(t);
        symbolQueueRepository.offer(s);
    }

}