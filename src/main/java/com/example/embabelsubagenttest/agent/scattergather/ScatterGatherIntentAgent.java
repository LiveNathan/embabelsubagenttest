package com.example.embabelsubagenttest.agent.scattergather;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Routes user requests to appropriate specialist agents.
 * Uses pattern matching on UserIntent sealed interface for routing.
 * CommandOrchestrator uses ScatterGatherBuilder internally for parallel service execution.
 */
@Agent(description = "Routes user requests to the appropriate specialist agent")
public class ScatterGatherIntentAgent {

    private final CommandOrchestrator commandOrchestrator;
    private final ScatterGatherQueryAgent queryAgent;
    private final AgentPlatform agentPlatform;

    public ScatterGatherIntentAgent(CommandOrchestrator commandOrchestrator, ScatterGatherQueryAgent queryAgent, AgentPlatform agentPlatform) {
        this.commandOrchestrator = commandOrchestrator;
        this.queryAgent = queryAgent;
        this.agentPlatform = agentPlatform;
    }

    @Action
    public UserIntent classifyIntent(UserInput userInput, Ai ai) {
        return ai.withAutoLlm()
                .creating(UserIntent.class)
                .fromPrompt(createClassifyIntentPrompt(userInput));
    }

    String createClassifyIntentPrompt(UserInput userInput) {
        return String.format("""
                        Classify the user's intent:
                        - COMMAND: User wants to see banana ASCII art, get a fortune cookie message, or hear a dad joke (single action)
                        - QUERY: User is asking a general question or requesting information (single question)
                        - MULTIPLE: User has BOTH a command AND a question (e.g., "show me a banana and tell me where they come from")
                        - UNKNOWN: User's intent is unclear or doesn't match the above categories
                        
                        User message: %s
                        
                        Examples:
                        - "Show me a banana" -> COMMAND
                        - "Where do bananas come from?" -> QUERY
                        - "Show me a banana and tell me where they come from" -> MULTIPLE
                        - "Tell me a joke and explain why it's funny" -> MULTIPLE
                        
                        Return:
                        - Command with a clear description of what they want (banana art, fortune, or joke)
                        - Query with the question they're asking
                        - Multiple with both commandDescription and queryQuestion filled in
                        - Unknown with the reason""",
                userInput.getContent()).trim();
    }

    // Route based on intent type - RunSubagent puts intent on blackboard for subagent to access
    @Action
    public AgentMessageResponse routeIntent(UserIntent intent) {
        return switch (intent) {
            case UserIntent.Command command -> RunSubagent
                    .fromAnnotatedInstance(commandOrchestrator, CommandOrchestrator.CommandOrchestratorResponse.class);
            case UserIntent.Query query -> RunSubagent
                    .fromAnnotatedInstance(queryAgent, ScatterGatherQueryAgent.QuerySubagentResponse.class);
            case UserIntent.Unknown unknown ->
                    new UnknownResponse("I'm not sure what you're asking for: " + unknown.reason());
            case UserIntent.Multiple multiple -> handleMultipleIntents(multiple);
        };
    }

    @Action
    public MultipleIntentsResult handleMultipleIntents(UserIntent.Multiple multiple) {
        List<CompletableFuture<AgentMessageResponse>> tasks = new ArrayList<>();

        // Add command task if present
        if (multiple.commandDescription() != null && !multiple.commandDescription().isBlank()) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                var agentWrapper = agentPlatform.agents().stream()
                        .filter(a -> a.getName().equals(commandOrchestrator.getClass().getSimpleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("CommandOrchestrator wrapper not found"));

                AgentProcess agentProcess = agentPlatform.createAgentProcessFrom(
                        agentWrapper,
                        ProcessOptions.DEFAULT,
                        new UserIntent.Command(multiple.commandDescription())
                );
                AgentProcess completedProcess = agentProcess.run();
                return completedProcess.last(AgentMessageResponse.class);
            }));
        }

        // Add query task if present
        if (multiple.queryQuestion() != null && !multiple.queryQuestion().isBlank()) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                var agentWrapper = agentPlatform.agents().stream()
                        .filter(a -> a.getName().equals(queryAgent.getClass().getSimpleName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("ScatterGatherQueryAgent wrapper not found"));

                AgentProcess agentProcess = agentPlatform.createAgentProcessFrom(
                        agentWrapper,
                        ProcessOptions.DEFAULT,
                        new UserIntent.Query(multiple.queryQuestion())
                );
                AgentProcess completedProcess = agentProcess.run();
                return completedProcess.last(AgentMessageResponse.class);
            }));
        }

        // Wait for all tasks to complete and consolidate results
        List<AgentMessageResponse> responses = tasks.stream()
                .map(CompletableFuture::join)
                .toList();

        String consolidatedMessage = responses.stream()
                .map(AgentMessageResponse::message)
                .collect(Collectors.joining("\n\n---\n\n"));

        return new MultipleIntentsResult(consolidatedMessage, responses.size());
    }

    @Action
    public TranslatedResponse translateToPortuguese(AgentMessageResponse response, Ai ai) {
        String formatted = ai.withAutoLlm()
                .withId("translate-to-portuguese")
                .generateText("""
                        Translate the following response into Portuguese.
                        Keep the same tone and style, but make it natural Portuguese.
                        If there's ASCII art, keep it intact.
                        
                        Original response:
                        %s""".formatted(response.message()));
        return new TranslatedResponse(formatted);
    }

    @AchievesGoal(description = "User request satisfied")
    @Action
    public IntentAgentResponse complete(TranslatedResponse translated) {
        return new IntentAgentResponse(translated.message());
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "intent")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"),
            @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY"),
            @JsonSubTypes.Type(value = UserIntent.Unknown.class, name = "UNKNOWN"),
            @JsonSubTypes.Type(value = UserIntent.Multiple.class, name = "MULTIPLE")
    })
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {
        }

        record Query(String question) implements UserIntent {
        }

        record Unknown(String reason) implements UserIntent {
        }

        record Multiple(String commandDescription, String queryQuestion) implements UserIntent {
        }
    }

    public record IntentAgentResponse(String message) {
    }

    // Response types
    public record TranslatedResponse(String message) {
    }

    public record UnknownResponse(String message) implements AgentMessageResponse {
    }

    public record MultipleIntentsResult(String message, int responseCount) implements AgentMessageResponse {
    }
}
