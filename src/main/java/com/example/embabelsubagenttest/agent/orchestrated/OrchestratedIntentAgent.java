package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;

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
    public String routeIntent(String userMessage, ActionContext context) {
        UserIntent intent = classifyIntent(userMessage, context);

        return switch (intent) {
            case UserIntent.Command c -> {
                var response = commandAgent.handleCommand(c.description(), context);
                yield response.message();
            }
            case UserIntent.Query q -> {
                var response = queryAgent.answerUserQuestion(q.question(), context.ai());
                yield response.answer();
            }
            case UserIntent.Unknown u -> u.message();
        };
    }

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
