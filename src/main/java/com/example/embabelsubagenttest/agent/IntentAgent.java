package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RunSubagent;
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
    @JsonSubTypes({@JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"), @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY")})
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {
        }

        record Query(String question) implements UserIntent {
        }
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
                        - COMMAND: User wants to change or edit something like channel names, colors, and routes
                        - QUERY: User is asking a question about mixer's current state or requesting information
                        
                        User message: %s
                        
                        Return Command with a clear description of what they want to change, or Query with the question they're asking.""",
                userInput.getContent()).trim();
    }

    @Action
    public AgentMessageResponse handleIntent(UserIntent intent) {
        return switch (intent) {
            case UserIntent.Query query -> RunSubagent.fromAnnotatedInstance(queryAgent, AgentMessageResponse.class);
            case UserIntent.Command command -> RunSubagent.fromAnnotatedInstance(commandAgent, AgentMessageResponse.class);
        };
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
