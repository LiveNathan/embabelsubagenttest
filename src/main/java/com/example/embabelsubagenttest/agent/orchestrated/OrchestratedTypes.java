package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.common.SomeOf;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Domain types for orchestrated channel editing.
 */
public class OrchestratedTypes {

    // --- Task Records (Requests) ---

    public record NameEditTask(int channelNumber, String newName) {
    }

    public record ColorEditTask(int channelNumber, String hexColor) {
    }

    public record RouteEditTask(int channelNumber, String destination) {
    }

    /**
     * EditPlan implements SomeOf - LLM populates applicable fields.
     * Allows multiple edits across different channels and types in one plan.
     */
    public record EditPlan(
            @Nullable List<NameEditTask> nameEdits,
            @Nullable List<ColorEditTask> colorEdits,
            @Nullable List<RouteEditTask> routeEdits
    ) implements SomeOf {
        public boolean isEmpty() {
            return (nameEdits == null || nameEdits.isEmpty()) &&
                   (colorEdits == null || colorEdits.isEmpty()) &&
                   (routeEdits == null || routeEdits.isEmpty());
        }
    }

    // --- Result Records ---

    public record NameEditResult(
            int channelNumber,
            @Nullable String message,
            @Nullable String errorMessage
    ) {
        public static NameEditResult success(int channelNumber, String message) {
            return new NameEditResult(channelNumber, message, null);
        }

        public static NameEditResult error(int channelNumber, String message) {
            return new NameEditResult(channelNumber, null, message);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    public record ColorEditResult(
            int channelNumber,
            @Nullable String message,
            @Nullable String errorMessage
    ) {
        public static ColorEditResult success(int channelNumber, String message) {
            return new ColorEditResult(channelNumber, message, null);
        }

        public static ColorEditResult error(int channelNumber, String message) {
            return new ColorEditResult(channelNumber, null, message);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    public record RouteEditResult(
            int channelNumber,
            @Nullable String message,
            @Nullable String errorMessage
    ) {
        public static RouteEditResult success(int channelNumber, String message) {
            return new RouteEditResult(channelNumber, message, null);
        }

        public static RouteEditResult error(int channelNumber, String message) {
            return new RouteEditResult(channelNumber, null, message);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    /**
     * Aggregated results of the edit plan execution.
     */
    public record EditResults(
            List<NameEditResult> nameResults,
            List<ColorEditResult> colorResults,
            List<RouteEditResult> routeResults
    ) {
    }
}
