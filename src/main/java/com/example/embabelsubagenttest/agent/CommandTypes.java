package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.common.SomeOf;
import org.springframework.lang.Nullable;

/**
 * Shared types for command orchestration.
 * Uses SomeOf pattern to allow LLM to determine which services to invoke.
 */
public class CommandTypes {

    // Request types (input to services)
    public record BananaArtRequest(String description) {}

    public record FortuneRequest(String description) {}

    public record JokeRequest(String description) {}

    /**
     * CommandRequest implements SomeOf - LLM populates applicable fields.
     * This allows a single LLM call to determine which services should be invoked.
     */
    public record CommandRequest(
            @Nullable BananaArtRequest bananaArt,
            @Nullable FortuneRequest fortune,
            @Nullable JokeRequest joke
    ) implements SomeOf {
        public boolean isEmpty() {
            return bananaArt == null && fortune == null && joke == null;
        }
    }

    // Result types (output from services)
    public record BananaArtResult(
            @Nullable String art,
            @Nullable String errorMessage
    ) {
        public static BananaArtResult success(String art) {
            return new BananaArtResult(art, null);
        }

        public static BananaArtResult error(String message) {
            return new BananaArtResult(null, message);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    public record FortuneResult(
            @Nullable String fortune,
            @Nullable String errorMessage
    ) {
        public static FortuneResult success(String fortune) {
            return new FortuneResult(fortune, null);
        }

        public static FortuneResult error(String message) {
            return new FortuneResult(null, message);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    public record JokeResult(
            @Nullable String joke,
            @Nullable String errorMessage
    ) {
        public static JokeResult success(String joke) {
            return new JokeResult(joke, null);
        }

        public static JokeResult error(String message) {
            return new JokeResult(null, message);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    /**
     * Aggregated results from parallel execution.
     * Contains results from all invoked services.
     */
    public record CommandResults(
            @Nullable BananaArtResult bananaArt,
            @Nullable FortuneResult fortune,
            @Nullable JokeResult joke
    ) {}
}
