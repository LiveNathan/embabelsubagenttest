package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Entry point agent for the orchestrated editing pattern.
 * Classifies user intent and routes to specialized logic or sub-agents.
 */
@Agent(description = "Orchestrates user requests for mixing console adjustments or inquiries")
public class OrchestratedIntentAgent {

    private final ChannelEditOrchestratorAgent orchestratorAgent;
    private final StudioConsole studioConsole;

    public OrchestratedIntentAgent(ChannelEditOrchestratorAgent orchestratorAgent, StudioConsole studioConsole) {
        this.orchestratorAgent = orchestratorAgent;
        this.studioConsole = studioConsole;
    }

    @Action
    public UserIntent classifyIntent(UserInput userInput, Ai ai) {
        return ai.withAutoLlm()
                .creating(UserIntent.class)
                .fromPrompt(String.format("""
                        Classify the user's intent regarding the studio mixing console:
                        - COMMAND: The user wants to change settings, rename channels, change colors, or update routing.
                        - QUERY: The user is asking about the current state of the console or how it is configured.
                        
                        User message: %s
                        
                        Return:
                        - Command with a description of the requested changes.
                        - Query with the specific question being asked.
                        """, userInput.getContent()));
    }

    @Action
    public OrchestratedResponse routeIntent(UserIntent intent, Ai ai) {
        return switch (intent) {
            case UserIntent.Command command -> {
                // Delegate to the orchestrator subagent
                // Note: Assuming OrchestratorResponse is the expected return type from ChannelEditOrchestratorAgent
                yield RunSubagent.fromAnnotatedInstance(orchestratorAgent, OrchestratedResponse.class);
            }
            case UserIntent.Query query -> {
                // Handle simple queries directly using the console state and LLM
                String answer = ai.withAutoLlm().generateText(String.format("""
                        You are a studio assistant. Use the current console state to answer the user's question.
                        
                        Console State:
                        %s
                        
                        User Question: %s
                        """, studioConsole.describeChannels(), query.question()));
                yield new OrchestratedResponse(answer);
            }
        };
    }

    @AchievesGoal(description = "The user's request has been processed")
    @Action
    public FinalResponse done(OrchestratedResponse response) {
        return new FinalResponse(response.message());
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"),
            @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY")
    })
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {}
        record Query(String question) implements UserIntent {}
    }

    public record OrchestratedResponse(String message) {}

    public record FinalResponse(String message) {}
}
