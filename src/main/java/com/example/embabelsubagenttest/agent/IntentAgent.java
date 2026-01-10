package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Agent(description = "Routes user requests to the appropriate specialist agent")
public class IntentAgent {
    private final QueryAgent queryAgent;
    private final CommandAgent commandAgent;
    private final AgentPlatform agentPlatform;

    public IntentAgent(QueryAgent queryAgent, CommandAgent commandAgent, AgentPlatform agentPlatform) {
        this.queryAgent = queryAgent;
        this.commandAgent = commandAgent;
        this.agentPlatform = agentPlatform;
    }

    public record IntentAgentResponse(String message) {
    }

    String createClassifyIntentPrompt(UserInput userInput) {
        return String.format("""
                        Classify the user's intent:
                        - COMMAND: User wants to change or edit something like channel names, colors, and routes (single request)
                        - QUERY: User is asking a question about mixer's current state or requesting information (single request)
                        - COMPOSITE: User has multiple requests that combine commands and/or queries
                        
                        User message: %s
                        
                        For COMMAND: Return with a clear description of what they want to change
                        For QUERY: Return with the question they're asking
                        For COMPOSITE: Return with lists of commands and queries. Parse out each distinct request.
                        
                        Examples of COMPOSITE:
                        - "Show me a banana and tell me where they come from" -> commands: [banana art], queries: [where do bananas come from]
                        - "Give me a fortune cookie and a dad joke" -> commands: [fortune cookie, dad joke], queries: []
                        - "What is the mixer state and show me a banana" -> commands: [banana art], queries: [mixer state]""",
                userInput.getContent()).trim();
    }

    @Action
    public UserIntent classifyIntent(UserInput userInput, Ai ai) {
        return ai.withAutoLlm()
                .creating(UserIntent.class)
                .fromPrompt(createClassifyIntentPrompt(userInput));
    }

    /**
     * Expands composite intent into individual intents and processes them in parallel
     */
    @Action
    public CompositeIntentResult handleCompositeIntent(UserIntent.Composite composite, OperationContext context) {
        List<CompletableFuture<AgentMessageResponse>> tasks = new ArrayList<>();

        // Process commands in parallel
        for (UserIntent.Command command : composite.commands()) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                // Find the Agent wrapper for our injected commandAgent bean
                var agentWrapper = agentPlatform.agents().stream()
                        .filter(a -> a.getName().equals(commandAgent.getClass().getSimpleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("CommandAgent wrapper not found"));

                var agentProcess = agentPlatform.createAgentProcessFrom(
                        agentWrapper,
                        ProcessOptions.DEFAULT,
                        command
                );
                var completedProcess = agentProcess.run();
                return completedProcess.last(AgentMessageResponse.class);
            }));
        }

        // Process queries in parallel
        for (UserIntent.Query query : composite.queries()) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                // Find the Agent wrapper for our injected queryAgent bean
                var agentWrapper = agentPlatform.agents().stream()
                        .filter(a -> a.getName().equals(queryAgent.getClass().getSimpleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("QueryAgent wrapper not found"));

                var agentProcess = agentPlatform.createAgentProcessFrom(
                        agentWrapper,
                        ProcessOptions.DEFAULT,
                        query
                );
                var completedProcess = agentProcess.run();
                return completedProcess.last(AgentMessageResponse.class);
            }));
        }

        // Wait for all tasks and collect responses
        List<AgentMessageResponse> responses = tasks.stream()
                .map(CompletableFuture::join)
                .toList();

        // Consolidate all responses
        String consolidatedMessage = responses.stream()
                .map(AgentMessageResponse::message)
                .collect(Collectors.joining("\n\n---\n\n"));

        return new CompositeIntentResult(consolidatedMessage, responses.size());
    }

    @Action
    public AgentMessageResponse handleCommand(UserIntent.Command command) {
        return RunSubagent.fromAnnotatedInstance(commandAgent, AgentMessageResponse.class);
    }

    @Action
    public AgentMessageResponse handleQuery(UserIntent.Query query) {
        return RunSubagent.fromAnnotatedInstance(queryAgent, AgentMessageResponse.class);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "intent")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"),
            @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY"),
            @JsonSubTypes.Type(value = UserIntent.Composite.class, name = "COMPOSITE")
    })
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {
        }

        record Query(String question) implements UserIntent {
        }

        record Composite(java.util.List<Command> commands, java.util.List<Query> queries) implements UserIntent {
        }
    }

    public record CompositeIntentResult(String message, int responseCount) implements AgentMessageResponse {
    }

    public record TranslatedResponse(String message) {
    }

    @Action
    public TranslatedResponse translateToPortuguese(AgentMessageResponse subagentResponse, Ai ai) {
        return ai.withAutoLlm()
                .withId("translate-to-portuguese")
                .creating(TranslatedResponse.class)
                .fromPrompt("""
                        Translate the following response into Portuguese.
                        Keep the same tone and style, but make it natural Portuguese.
                        If there's ASCII art, keep it intact.

                        Original response:
                        %s""".formatted(subagentResponse.message()));
    }

    @AchievesGoal(description = "User request satisfied")
    @Action
    public IntentAgentResponse done(TranslatedResponse translatedResponse) {
        return new IntentAgentResponse(translatedResponse.message());
    }


}
