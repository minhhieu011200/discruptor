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
    }

    @Override
    @Measured(value = "disruptor.event.process", description = "Time to process a market event in disruptor")
    @TraceLog("ProcessMarketEvent")
    public void process(SymbolRequestDTO data) {

        String imt = data.getImtcode();
        if (imt == null || imt.isEmpty() || "USDVND".equals(imt))
            return;

        processFxUsdFast(data, imt);
    }

    private void processFxUsdFast(SymbolRequestDTO data, String imt) {

        // === GET ORIGINAL ===
        SymbolEntity oldFxUsd = symbolRepository.get(imt);
        if (oldFxUsd == null)
            return;

        // === CLONE → MUTATE ===
        SymbolEntity fxUsd = SymbolEntity.cloneLight(oldFxUsd);
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
        fxUsd.setVersion(fxUsd.getVersion() + 1);

        // === PUT BACK ===
        symbolRepository.set(imt, fxUsd);
        fastTranslog(fxUsd);

        // -----------------------------
        // Không phải USD → bỏ qua cross
        // -----------------------------
        final String sellCcy = data.getSellCurrency();
        final String buyCcy = data.getBuyCurrency();

        boolean buyIsUsd = "USD".equals(sellCcy);
        boolean sellIsUsd = "USD".equals(buyCcy);
        if (!buyIsUsd && !sellIsUsd)
            return;

        // === Lấy USDVND ===
        SymbolEntity usdVndSrc = symbolRepository.get("USDVND");
        if (usdVndSrc == null)
            return;

        SymbolEntity usdVnd = SymbolEntity.cloneLight(usdVndSrc);
        // === Tính tên cross ===
        String crossImt = buyIsUsd ? buyCcy + "VND" : "VND" + sellCcy;

        SymbolEntity oldFxVnd = symbolRepository.get(crossImt);
        SymbolEntity fxVnd = (oldFxVnd == null) ? new SymbolEntity() : SymbolEntity.cloneLight(oldFxVnd);

        // === CROSS RATE ===
        if (buyIsUsd) {
            fxVnd.setBid(data.getBid() * usdVnd.getBid());
            fxVnd.setAsk(data.getAsk() * usdVnd.getAsk());
            fxVnd.setBuyCurrency(buyCcy);
        } else {
            fxVnd.setBid(usdVnd.getAsk() / data.getBid());
            fxVnd.setAsk(usdVnd.getBid() / data.getAsk());
            fxVnd.setBuyCurrency(sellCcy);
        }

        fxVnd.setSellCurrency("VND");
        fxVnd.setTenor(data.getTenor());
        fxVnd.setImtCode();
        fxVnd.setValidFrom(data.getValidFrom());
        fxVnd.setValidTill(data.getValidTill());
        fxVnd.setRateQuoteID(data.getRateQuoteID());
        fxVnd.setVersion(fxVnd.getVersion() + 1);

        // === SỬA Lỗi Ở Đây ===
        // Bạn viết: fxUsd.set(crossImt, fxVnd); → SAI
        symbolRepository.set(crossImt, fxVnd);

        fastTranslog(fxVnd);
    }

    private void fastTranslog(SymbolEntity s) {
        TranslogEntity t = new TranslogEntity();

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