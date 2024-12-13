/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.injection.chat;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import java.time.Duration;
import java.util.List;

public class WildFlyMistralAiChatModelLanguage implements WildFlyChatModelConfig {

    private String key;
    private String baseUrl;
    private Boolean logRequests;
    private Boolean logResponses;
    private Integer maxTokens;
    private String modelName;
    private Integer randomSeed;
    private Boolean safePrompt;
    private Double temperature;
    private long connectTimeOut;
    private Double topP;
    private boolean isJson;
    private boolean streaming;

    @Override
    public ChatLanguageModel createLanguageModel(List<ChatModelListener> listeners) {
        MistralAiChatModel.MistralAiChatModelBuilder builder = MistralAiChatModel.builder()
                .apiKey(key)
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .maxRetries(5)
                .maxTokens(maxTokens)
                .modelName(modelName)
                .randomSeed(randomSeed)
                .safePrompt(safePrompt)
                .temperature(temperature)
                .timeout(Duration.ofMillis(connectTimeOut))
                .topP(topP);
        if (isJson) {
            builder.responseFormat("json_object");
        }
        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingLanguageModel(List<ChatModelListener> listeners) {
        MistralAiStreamingChatModel.MistralAiStreamingChatModelBuilder builder = MistralAiStreamingChatModel.builder()
                .apiKey(key)
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .maxTokens(maxTokens)
                .modelName(modelName)
                .randomSeed(randomSeed)
                .safePrompt(safePrompt)
                .temperature(temperature)
                .timeout(Duration.ofMillis(connectTimeOut))
                .topP(topP);
        if (isJson) {
            builder.responseFormat("json_object");
        }
        return builder.build();
    }

    public WildFlyMistralAiChatModelLanguage apiKey(String key) {
        this.key = key;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage logRequests(Boolean logRequests) {
        this.logRequests = logRequests;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage logResponses(Boolean logResponses) {
        this.logResponses = logResponses;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage randomSeed(Integer randomSeed) {
        this.randomSeed = randomSeed;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage safePrompt(Boolean safePrompt) {
        this.safePrompt = safePrompt;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage setJson(boolean isJson) {
        this.isJson = isJson;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage timeout(long timeOut) {
        this.connectTimeOut = timeOut;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage topP(Double topP) {
        this.topP = topP;
        return this;
    }

    public WildFlyMistralAiChatModelLanguage streaming(boolean streaming) {
        this.streaming = streaming;
        return this;
    }

    @Override
    public boolean isStreaming() {
        return streaming;
    }
}
