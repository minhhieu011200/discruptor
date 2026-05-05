package com.example.demo.application.service;

import java.nio.ByteBuffer;

import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.etrade.gateway.sbe.BooleanType;
import com.etrade.gateway.sbe.MessageHeaderDecoder;
import com.etrade.gateway.sbe.QuoteDecoder;
import com.example.demo.application.annotation.Measured;
import com.example.demo.application.annotation.TraceLog;
import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.service.ProcessMarketParseService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessMarketParseSBE implements ProcessMarketParseService {

    @Override
    @Measured(value = "parser.sbe.process", description = "Time to parse SBE message into DTO")
    @TraceLog("ProcessMarketParseSBE")
    public SymbolRequestDTO process(byte[] data, String traceId, Long startTime) {
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.wrap(data));
        // 1. Decode header
        MessageHeaderDecoder header = new MessageHeaderDecoder();
        header.wrap(buffer, 0);
        QuoteDecoder quoteDecoder = new QuoteDecoder();
        try {
            quoteDecoder.wrap(
                    buffer,
                    header.encodedLength(), // offset body
                    header.blockLength(),
                    header.version());

            double bid = quoteDecoder.bid();
            double ask = quoteDecoder.ask();
            BooleanType validEnum = quoteDecoder.valid();
            long validFrom = quoteDecoder.validFrom();
            long validTill = quoteDecoder.validTill();
            String rateType = quoteDecoder.rateType();
            String rateQuoteID = quoteDecoder.rateQuoteID();
            String rateCategoryID = quoteDecoder.rateCategoryID();

            String baseCurrency = quoteDecoder.baseCurrency();
            String quoteCurrency = quoteDecoder.quoteCurrency();
            String tenor = quoteDecoder.tenor();
            String status = quoteDecoder.status();
            SymbolRequestDTO symbolRequestDTO = new SymbolRequestDTO();
            symbolRequestDTO.hydrate(
                    bid,
                    ask,
                    validEnum == BooleanType.TRUE,
                    validFrom,
                    validTill,
                    rateType,
                    rateQuoteID,
                    rateCategoryID,
                    baseCurrency,
                    quoteCurrency,
                    tenor,
                    status);
            symbolRequestDTO.setTraceId(traceId);
            symbolRequestDTO.setStartTime(startTime);

            if (log.isTraceEnabled()) {
                log.trace(
                        "Decoded Quote: symbolRequestDTO={}",
                        symbolRequestDTO.toString());
            }
            return symbolRequestDTO;
        } catch (IndexOutOfBoundsException ex) {
            log.error("Failed to decode message" + ex);
            return null;
        }
    }

}
