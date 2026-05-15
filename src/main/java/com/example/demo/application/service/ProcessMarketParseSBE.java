package com.example.demo.application.service;

import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.etrade.gateway.sbe.BooleanType;
import com.etrade.gateway.sbe.MessageHeaderDecoder;
import com.etrade.gateway.sbe.QuoteDecoder;
import com.example.demo.application.annotation.Measured;
import com.example.demo.application.dto.SymbolRequestDTO;
import com.example.demo.domain.service.ProcessMarketParseService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessMarketParseSBE implements ProcessMarketParseService {

    // PERF: Reuse decoder objects per-thread – tránh allocation mỗi message
    private static final ThreadLocal<MessageHeaderDecoder> HEADER_TL = ThreadLocal
            .withInitial(MessageHeaderDecoder::new);
    private static final ThreadLocal<QuoteDecoder> QUOTE_TL = ThreadLocal.withInitial(QuoteDecoder::new);
    private static final ThreadLocal<UnsafeBuffer> BUFFER_TL = ThreadLocal
            .withInitial(() -> new UnsafeBuffer(new byte[0]));

    @Override
    @Measured(value = "parser.sbe.process", description = "Time to parse SBE message into DTO")
    public SymbolRequestDTO process(byte[] data, String traceId, Long startTime) {
        // PERF: zero-copy wrap – không tạo ByteBuffer trước
        UnsafeBuffer buffer = BUFFER_TL.get();
        buffer.wrap(data, 0, data.length);

        MessageHeaderDecoder header = HEADER_TL.get();
        header.wrap(buffer, 0);

        QuoteDecoder quoteDecoder = QUOTE_TL.get();
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
