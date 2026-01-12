package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.example.embabelsubagenttest.agent.AgentMessageResponse;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Top-level agent that classifies user intent and routes to either
 * OrchestratedCommandAgent (for actions) or OrchestratedQueryAgent (for information).
 */
@Agent(description = "Routes user requests to either a command orchestrator or a query agent")
public class OrchestratedIntentAgent {

    private final OrchestratedCommandAgent commandAgent;
    private final OrchestratedQueryAgent queryAgent;

    public OrchestratedIntentAgent(
            OrchestratedCommandAgent commandAgent,
            OrchestratedQueryAgent queryAgent) {
        this.commandAgent = commandAgent;
        this.queryAgent = queryAgent;
    }

    @Action
    public UserIntent classifyIntent(UserInput userInput, Ai ai) {
        return ai.withAutoLlm()
                .withId("classify-orchestrated-intent")
                .creating(UserIntent.class)
                .fromPrompt("""
                        Classify the user's intent into one of the following:

                        - COMMAND: The user wants to perform an action like seeing a banana, hearing a joke, or getting a fortune.
                        - QUERY: The user is asking a general question or seeking information.
                        - UNKNOWN: The intent is unclear.

                        For COMMAND, provide a description of what they want.
                        For QUERY, provide the question they're asking.
                        For UNKNOWN, provide a message explaining why it's unclear.

                        User message: %s""".formatted(userInput.getContent()));
    }

    @Action
    public AgentMessageResponse routeIntent(UserIntent intent) {
        return switch (intent) {
            case UserIntent.Command c ->
                    RunSubagent.fromAnnotatedInstance(commandAgent, OrchestratedCommandAgent.OrchestratedResponse.class);
            case UserIntent.Query q ->
                    RunSubagent.fromAnnotatedInstance(queryAgent, OrchestratedQueryAgent.QueryResponse.class);
            case UserIntent.Unknown u -> new UnknownResponse(u.message());
        };
    }

    @AchievesGoal(description = "User request satisfied")
    @Action
    public FinalResponse done(AgentMessageResponse response) {
        return new FinalResponse(response.message());
    }

    public record FinalResponse(String message) {}

    public record UnknownResponse(String message) implements AgentMessageResponse {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "intent")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"),
            @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY"),
            @JsonSubTypes.Type(value = UserIntent.Unknown.class, name = "UNKNOWN")
    })
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {}
        record Query(String question) implements UserIntent {}
        record Unknown(String message) implements UserIntent {}
    }
}
