package com.example.embabelsubagenttest.agent.chatbot;

import com.embabel.agent.api.common.SomeOf;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.lang.Nullable;

/**
 * Domain types for the chatbot pattern.
 * Uses sealed interfaces for type-safe intent classification.
 */
public class ChatbotTypes {

    /**
     * Intent classification for incoming user messages.
     * Used by the LLM to determine which action should handle the request.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "intent")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ChatIntent.BananaArt.class, name = "BANANA_ART"),
            @JsonSubTypes.Type(value = ChatIntent.Fortune.class, name = "FORTUNE"),
            @JsonSubTypes.Type(value = ChatIntent.Joke.class, name = "JOKE"),
            @JsonSubTypes.Type(value = ChatIntent.GeneralQuery.class, name = "GENERAL_QUERY"),
            @JsonSubTypes.Type(value = ChatIntent.Multiple.class, name = "MULTIPLE")
    })
    public sealed interface ChatIntent {
        record BananaArt(String description) implements ChatIntent {
        }

        record Fortune(String description) implements ChatIntent {
        }

        record Joke(String description) implements ChatIntent {
        }

        record GeneralQuery(String question) implements ChatIntent {
        }

        record Multiple(
                @Nullable String bananaArt,
                @Nullable String fortune,
                @Nullable String joke
        ) implements ChatIntent, SomeOf {
            public boolean isEmpty() {
                return bananaArt == null && fortune == null && joke == null;
            }
        }
    }

    /**
     * Request for multiple services.
     * Implements SomeOf - LLM populates only applicable fields.
     */
    public record MultiServiceRequest(
            @Nullable String bananaArt,
            @Nullable String fortune,
            @Nullable String joke
    ) implements SomeOf {
        public boolean isEmpty() {
            return bananaArt == null && fortune == null && joke == null;
        }
    }
}
