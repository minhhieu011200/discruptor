package com.example.demo.application.service;

import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.entity.SymbolEntity;
import com.example.demo.domain.entity.TranslogEntity;
import com.example.demo.domain.repository.SymbolRepository;
import com.example.demo.domain.repository.TranslogShardedQueueRepository;
import com.example.demo.domain.service.ProcessMarketEventService;
import org.springframework.stereotype.Service;

@Service
public class ProcessMarketEvent implements ProcessMarketEventService {
    private static final int POOL_SIZE = 64;

    private final ThreadLocal<TranslogEntity[]> translogPool = ThreadLocal.withInitial(() -> {
        TranslogEntity[] arr = new TranslogEntity[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE; i++) {
            arr[i] = new TranslogEntity(); // pre-allocate
        }
        return arr;
    });

    private final ThreadLocal<int[]> translogIndex = ThreadLocal.withInitial(() -> new int[] { 0 });

    // private final PublishService<Void, SymbolEntity> redisPublishService;
    // private final PublishService<Void, SymbolRequestDTO> marketDisruptor;

    private final SymbolRepository symbolRepository;
    private final TranslogShardedQueueRepository translogShardedQueueRepository;

    public ProcessMarketEvent(
            // @Qualifier("RedisPubSub") PublishService<Void, SymbolEntity>
            // redisPublishService,
            SymbolRepository symbolRepository,
            TranslogShardedQueueRepository translogShardedQueueRepository
    // @Qualifier("MarketDisruptor") PublishService<Void, SymbolRequestDTO>
    // marketDisruptor
    ) {

        // this.redisPublishService = redisPublishService;
        this.symbolRepository = symbolRepository;
        this.translogShardedQueueRepository = translogShardedQueueRepository;
        // this.marketDisruptor = marketDisruptor;
    }

    @Override
    @com.example.demo.application.annotation.Measured(value = "disruptor.event.process", description = "Time to process a market event in disruptor")
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

        // Inline update: nhanh hơn gọi hàm
        usdVnd.setBuyCurrency(data.getBuyCurrency());
        usdVnd.setSellCurrency(data.getSellCurrency());
        usdVnd.setBid(data.getBid());
        usdVnd.setAsk(data.getAsk());
        usdVnd.setTenor(data.getTenor());
        usdVnd.setStatus(data.getStatus());
        usdVnd.setImtCode();

        // treasury price
        usdVnd.setBuyPriceTesury(data.getAsk());
        usdVnd.setSellPriceTesury(data.getBid());

        fastTranslog(usdVnd);

        // Publish cho các symbol USDXXX, nhưng KHÔNG chứa VND
        // for (SymbolEntity symbol : symbolRepository.getAll().values()) { // dùng
        // fastValues() -> trả về Collection không
        // // wrap, nhanh hơn
        // String code = symbol.getImtcode();

        // // skip null + skip USDVND
        // if (code == null || code == usdVnd.getImtcode())
        // continue;

        // // code.contains("USD") && !code.contains("VND") — nhưng tránh
        // toLower/toUpper
        // if (code.contains("USD") && !code.contains("VND")) {
        // SymbolRequestDTO dto = new SymbolRequestDTO();
        // dto.hydrate(
        // symbol.getBid(), symbol.getAsk(),
        // true, 0, 0, "", "", "",
        // symbol.getBuyCurrency(), symbol.getSellCurrency(),
        // symbol.getTenor(), symbol.getStatus());

        // marketDisruptor.publish(code, dto);
        // }
        // }
    }

    private void processFxUsdFast(SymbolRequestDTO data, String imt) {
        SymbolEntity fxUsd = symbolRepository.get(imt);
        if (fxUsd == null) {
            return;
        }
        fxUsd.setBuyCurrency(data.getBuyCurrency());
        fxUsd.setSellCurrency(data.getSellCurrency());
        fxUsd.setBid(data.getBid());
        fxUsd.setAsk(data.getAsk());
        fxUsd.setTenor(data.getTenor());
        fxUsd.setStatus(data.getStatus());
        fxUsd.setImtCode();

        fxUsd.setBuyPriceTesury(data.getAsk() + 1);
        fxUsd.setSellPriceTesury(data.getBid() - 1);
        // System.out.println("FX-USD updated: " + fxUsd.getImtcode());
        fastTranslog(fxUsd);

        // Nếu không liên quan USD thì return sớm → tối ưu
        boolean buyIsUsd = "USD".equals(data.getSellCurrency());
        boolean sellIsUsd = "USD".equals(data.getBuyCurrency());
        if (!buyIsUsd && !sellIsUsd)
            return;

        SymbolEntity usdVnd = symbolRepository.get("USDVND");
        if (usdVnd == null)
            return;

        // avoid string concat
        String crossImt = "VND";
        SymbolEntity fxVnd = symbolRepository.get(crossImt);

        if (fxVnd == null) {
            fxVnd = new SymbolEntity();
            symbolRepository.set(crossImt, fxVnd);
        }

        if (buyIsUsd) {
            // EURUSD -> EURVND
            fxVnd.setBid(data.getBid() * usdVnd.getBid());
            fxVnd.setAsk(data.getAsk() * usdVnd.getAsk());
            fxVnd.setBuyCurrency(data.getBuyCurrency());
        } else {
            // USDJPY -> JPYVND
            fxVnd.setBid(usdVnd.getAsk() / data.getBid());
            fxVnd.setAsk(usdVnd.getBid() / data.getAsk());
            fxVnd.setBuyCurrency(data.getSellCurrency());
        }

        fxVnd.setSellCurrency("VND");
        fxVnd.setTenor(data.getTenor());
        fxVnd.setImtCode();

        fastTranslog(fxVnd);
    }

    private void fastTranslog(SymbolEntity s) {

        TranslogEntity[] pool = translogPool.get();
        int[] idxArr = translogIndex.get();
        int idx = idxArr[0];

        TranslogEntity t = pool[idx];
        idxArr[0] = (idx + 1) & (POOL_SIZE - 1); // ring modulo cực nhanh, yêu cầu POOL_SIZE là power of 2

        // Ghi vào object slot riêng biệt
        t.hydrate(
                s.getImtcode(),
                s.getBuyCurrency(), s.getSellCurrency(),
                s.getTenor(),
                s.getBid(), s.getAsk(),
                s.getSpread(),
                s.getBuyPriceTesury(),
                s.getSellPriceTesury());

        // Mỗi t là object riêng → không có overwrite
        translogShardedQueueRepository.offer(t);
    }

}