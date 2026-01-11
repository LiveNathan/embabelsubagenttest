package com.example.embabelsubagenttest.agent.scattergather;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Routes user requests to appropriate specialist agents.
 * Uses pattern matching on UserIntent sealed interface for routing.
 * CommandOrchestrator uses ScatterGatherBuilder internally for parallel service execution.
 */
@Agent(description = "Routes user requests to the appropriate specialist agent")
public class ScatterGatherIntentAgent {

    private final CommandOrchestrator commandOrchestrator;
    private final ScatterGatherQueryAgent queryAgent;

    public ScatterGatherIntentAgent(CommandOrchestrator commandOrchestrator, ScatterGatherQueryAgent queryAgent) {
        this.commandOrchestrator = commandOrchestrator;
        this.queryAgent = queryAgent;
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
            case UserIntent.Multiple multiple -> new MultipleIntentsResponse(
                    "Multiple intents detected: " + multiple.commandDescription() + " AND " + multiple.queryQuestion() +
                    ". Please ask for one thing at a time."
            );
        };
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

    public record MultipleIntentsResponse(String message) implements AgentMessageResponse {
    }
}
