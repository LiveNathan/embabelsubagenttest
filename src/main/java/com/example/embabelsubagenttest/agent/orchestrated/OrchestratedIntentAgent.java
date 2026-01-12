package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.example.embabelsubagenttest.agent.AgentMessageResponse;

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
    public Object routeIntent(String userMessage, ActionContext context) {
        UserIntent intent = classifyIntent(userMessage, context);

        return switch (intent) {
            case UserIntent.Command c ->
                    RunSubagent.fromAnnotatedInstance(commandAgent, OrchestratedCommandAgent.OrchestratedResponse.class);
            case UserIntent.Query q ->
                    RunSubagent.fromAnnotatedInstance(queryAgent, OrchestratedQueryAgent.QueryResponse.class);
            case UserIntent.Unknown u -> new OrchestratedCommandAgent.OrchestratedResponse(u.message());
        };
    }

    @AchievesGoal(description = "User request satisfied")
    @Action
    public FinalResponse done(AgentMessageResponse response) {
        return new FinalResponse(response.message());
    }

    public record FinalResponse(String message) {}

    private UserIntent classifyIntent(String message, ActionContext context) {
        return context.ai().withAutoLlm()
                .withId("classify-orchestrated-intent")
                .creating(UserIntent.class)
                .fromPrompt("""
                        Classify the user's intent into one of the following:

                        - Command: The user wants to perform an action like seeing a banana, hearing a joke, or getting a fortune.
                        - Query: The user is asking a general question or seeking information.
                        - Unknown: The intent is unclear.

                        User message: %s""".formatted(message));
    }

    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {}
        record Query(String question) implements UserIntent {}
        record Unknown(String message) implements UserIntent {}
    }
}
