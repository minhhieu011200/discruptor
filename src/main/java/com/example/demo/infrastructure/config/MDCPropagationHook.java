package com.example.demo.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.Map;

/**
 * Propagate toàn bộ entries trong Reactor Context key "mdc" vào SLF4J MDC.
 *
 * Cách dùng — trong filter/interceptor, đưa bất kỳ field nào vào Context:
 *
 *   .contextWrite(ctx -> ctx.put("mdc", Map.of(
 *       "traceId",   tid,
 *       "userId",    userId,
 *       "accountId", accountId
 *   )))
 *
 * Sau đó logback pattern %X{traceId} / %X{userId} / %X{accountId}
 * tự động có giá trị ở mọi log statement downstream.
 */
@Configuration
public class MDCPropagationHook {

    public static final String MDC_CONTEXT_KEY = "mdc";
    private static final String HOOK_KEY        = "MDC-Propagation";

    @PostConstruct
    public void install() {
        Hooks.onEachOperator(HOOK_KEY,
                Operators.lift((scannable, subscriber) -> new MDCContextLifter<>(subscriber)));
    }

    @PreDestroy
    public void uninstall() {
        Hooks.resetOnEachOperator(HOOK_KEY);
    }

    // ── Inner lifter ──────────────────────────────────────────────────────────

    private static class MDCContextLifter<T> implements CoreSubscriber<T> {

        private final CoreSubscriber<T> delegate;

        MDCContextLifter(CoreSubscriber<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext();
        }

        @Override
        public void onSubscribe(Subscription s) {
            copyToMDC();
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            copyToMDC();
            delegate.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            copyToMDC();
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            copyToMDC();
            delegate.onComplete();
        }

        /**
         * Đọc Map từ Context key "mdc" và copy toàn bộ entries vào MDC.
         * Nếu không có key "mdc" trong Context → xóa toàn bộ MDC để tránh rò rỉ.
         */
        @SuppressWarnings("unchecked")
        private void copyToMDC() {
            Context ctx = delegate.currentContext();
            if (ctx.hasKey(MDC_CONTEXT_KEY)) {
                Map<String, String> mdcMap = ctx.get(MDC_CONTEXT_KEY);
                mdcMap.forEach(MDC::put);
            } else {
                MDC.clear();
            }
        }
    }
}
