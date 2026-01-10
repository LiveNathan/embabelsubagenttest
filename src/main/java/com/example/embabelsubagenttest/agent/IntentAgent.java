package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.State;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Agent(description = "Routes user requests to the appropriate specialist agent")
public class IntentAgent {
    private final QueryAgent queryAgent;
    private final CommandAgent commandAgent;

    public IntentAgent(QueryAgent queryAgent, CommandAgent commandAgent) {
        this.queryAgent = queryAgent;
        this.commandAgent = commandAgent;
    }

    public record IntentAgentResponse(String message) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "intent")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"),
            @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY"),
            @JsonSubTypes.Type(value = UserIntent.Unknown.class, name = "UNKNOWN")
    })
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {
        }

        record Query(String question) implements UserIntent {
        }

        record Unknown(String reason) implements UserIntent {
        }
    }

    @State
    public sealed interface IntentState {
    }

    @State
    public record QueryState(UserIntent.Query query) implements IntentState {
        @Action
        public FinalState processQuery(Ai ai) {
            QueryAgent.QuerySubagentResponse response = ai.withAutoLlm()
                    .withId("respond-to-query")
                    .creating(QueryAgent.QuerySubagentResponse.class)
                    .fromPrompt("""
                            You are a helpful assistant. Answer the user's question.

                            User question: %s""".formatted(query.question()));
            return new FinalState(response.message());
        }
    }

    @State
    public record CommandState(UserIntent.Command command) implements IntentState {
        @Action
        public FinalState processCommand(CommandAgent commandAgent, Ai ai) {
            CommandAgent.CommandAgentResponse response = commandAgent.handle(command, ai);
            return new FinalState(response.message());
        }
    }

    @State
    public record UnknownState(UserIntent.Unknown unknown) implements IntentState {
        @Action
        public FinalState handleUnknown() {
            return new FinalState("I'm not sure what you're asking for: " + unknown.reason());
        }
    }

    @State
    public record FinalState(String message) implements IntentState {
        @AchievesGoal(description = "User request satisfied")
        @Action
        public IntentAgentResponse complete() {
            return new IntentAgentResponse(message);
        }
    }

    @Action
    public IntentState classifyAndRoute(UserInput userInput, Ai ai) {
        UserIntent intent = ai.withAutoLlm()
                .creating(UserIntent.class)
                .fromPrompt(createClassifyIntentPrompt(userInput));

        return switch (intent) {
            case UserIntent.Query query -> new QueryState(query);
            case UserIntent.Command command -> new CommandState(command);
            case UserIntent.Unknown unknown -> new UnknownState(unknown);
        };
    }

    String createClassifyIntentPrompt(UserInput userInput) {
        return String.format("""
                        Classify the user's intent:
                        - COMMAND: User wants to see banana ASCII art, get a fortune cookie message, or hear a dad joke
                        - QUERY: User is asking a general question or requesting information
                        - UNKNOWN: User's intent is unclear or doesn't match the above categories

                        User message: %s

                        Return Command with a clear description of what they want (banana art, fortune, or joke), Query with the question they're asking, or Unknown with the reason.""",
                userInput.getContent()).trim();
    }
}
