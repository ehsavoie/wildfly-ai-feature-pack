/*
 * Copyright 2024 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.ai.injection.observability;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import java.util.concurrent.atomic.AtomicBoolean;

@Dependent
public class OpenTelemetryChatModelListener implements ChatModelListener {

    private static final String OTEL_SCOPE_KEY_NAME = "OTelScope";
    private static final String OTEL_SPAN_KEY_NAME = "OTelSpan";
    private final AtomicBoolean checkTracer = new AtomicBoolean(true);

    private Tracer tracer;

    public OpenTelemetryChatModelListener(Tracer tracer) {
        this.tracer = tracer;
    }

    private Tracer getTracer() {
        System.out.println("Tracer: " + tracer);
        if (tracer == null && checkTracer.getAndSet(false)) {
            Instance<Tracer> instance = jakarta.enterprise.inject.spi.CDI.current().select(Tracer.class);
            if (instance.isResolvable()) {
                this.tracer = instance.get();
            }
        }
        return tracer;
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        if (getTracer() == null) {
            return;
        }
        final ChatModelRequest request = requestContext.request();
        SpanBuilder spanBuilder = tracer.spanBuilder("chat " + request.model())
                .setParent(Context.current())
                .setAttribute("gen_ai.operation.name", "chat");
        if (request.maxTokens() != null) {
            spanBuilder.setAttribute("gen_ai.request.max_tokens", request.maxTokens());
        }

        if (request.temperature() != null) {
            spanBuilder.setAttribute("gen_ai.request.temperature", request.temperature());
        }

        if (request.topP() != null) {
            spanBuilder.setAttribute("gen_ai.request.top_p", request.topP());
        }
        if (request.messages() != null && !request.messages().isEmpty()) {
            spanBuilder.setAttribute("gen_ai.request.messages", request.messages().toString());
        }

        Span span = spanBuilder.startSpan();
        Scope scope = span.makeCurrent();
        System.out.println("OpenTelemetryChatModelListener.onRequest with context " + span.getSpanContext() + " and tracer " + tracer + " in thread " + Thread.currentThread() + " with scope " + scope);

        requestContext.attributes().put(OTEL_SCOPE_KEY_NAME, scope);
        requestContext.attributes().put(OTEL_SPAN_KEY_NAME, span);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Span span = (Span) responseContext.attributes().get(OTEL_SPAN_KEY_NAME);
        if (span != null) {
            ChatModelResponse response = responseContext.response();
            span.setAttribute("gen_ai.response.id", response.id())
                    .setAttribute("gen_ai.response.model", response.model());
            if (response.finishReason() != null) {
                span.setAttribute("gen_ai.response.finish_reasons", response.finishReason().toString());
            }
            TokenUsage tokenUsage = response.tokenUsage();
            if (tokenUsage != null) {
                span.setAttribute("gen_ai.usage.output_tokens", tokenUsage.outputTokenCount())
                        .setAttribute("gen_ai.usage.input_tokens", tokenUsage.inputTokenCount());
            }
            if (response.aiMessage() != null) {
                span.setAttribute("gen_ai.response.message", response.aiMessage().toString());
            }
            span.end();
        }
        Scope scope = (Scope) responseContext.attributes().get(OTEL_SCOPE_KEY_NAME);
        System.out.println("OpenTelemetryChatModelListener.onResponse with context " + span.getSpanContext() + " and tracer " + tracer + " in thread " + Thread.currentThread() + " with scope " + scope);
        closeScope(scope);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        Span span = (Span) errorContext.attributes().get(OTEL_SPAN_KEY_NAME);
        if (span != null) {
            span.recordException(errorContext.error());
            span.end();
        }
        closeScope((Scope) errorContext.attributes().get(OTEL_SCOPE_KEY_NAME));
    }

    private void closeScope(Scope scope) {
        if (scope != null) {
            System.out.println("OpenTelemetryChatModelListener.closeScope tracer " + tracer + " in thread " + Thread.currentThread() + " with scope " + scope);
            scope.close();
        }
    }
}
