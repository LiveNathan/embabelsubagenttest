package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.*;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.SomeOf;
import com.embabel.agent.api.common.workflow.control.ScatterGatherBuilder;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Routes user requests to appropriate specialist agents.
 * Refactored to use ScatterGatherBuilder for parallel execution and CommandOrchestrator for commands.
 */
@Agent(description = "Routes user requests to the appropriate specialist agent")
public class IntentAgent {

    private final CommandOrchestrator commandOrchestrator;
    private final QueryAgent queryAgent;

    public IntentAgent(CommandOrchestrator commandOrchestrator, QueryAgent queryAgent) {
        this.commandOrchestrator = commandOrchestrator;
        this.queryAgent = queryAgent;
    }

    public record IntentAgentResponse(String message) {}

    @Action
    public IntentState classifyAndRoute(UserInput userInput, Ai ai) {
        UserIntent intent = ai.withAutoLlm()
                .creating(UserIntent.class)
                .fromPrompt(createClassifyIntentPrompt(userInput));

        return switch (intent) {
            case UserIntent.Query query -> new QueryState(query, queryAgent);
            case UserIntent.Command command -> new CommandState(command, commandOrchestrator);
            case UserIntent.Unknown unknown -> new UnknownState(unknown);
            case UserIntent.Multiple multiple -> new MultiIntentState(
                    new MultipleIntents(
                            new UserIntent.Command(multiple.commandDescription()),
                            new UserIntent.Query(multiple.queryQuestion())
                    ),
                    commandOrchestrator,
                    queryAgent
            );
        };
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

    @State
    public sealed interface IntentState {}

    @State
    public record QueryState(UserIntent.Query query, QueryAgent queryAgent) implements IntentState {
        @Action
        public PreFormatState processQuery(ActionContext context) {
            QueryAgent.QuerySubagentResponse response = RunSubagent
                    .fromAnnotatedInstance(queryAgent, QueryAgent.QuerySubagentResponse.class);
            return new PreFormatState(response.message());
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "intent")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"),
            @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY"),
            @JsonSubTypes.Type(value = UserIntent.Unknown.class, name = "UNKNOWN"),
            @JsonSubTypes.Type(value = UserIntent.Multiple.class, name = "MULTIPLE")
    })
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {}
        record Query(String question) implements UserIntent {}
        record Unknown(String reason) implements UserIntent {}
        record Multiple(String commandDescription, String queryQuestion) implements UserIntent {}
    }

    @State
    public record UnknownState(UserIntent.Unknown unknown) implements IntentState {
        @Action
        public PreFormatState handleUnknown() {
            return new PreFormatState("I'm not sure what you're asking for: " + unknown.reason());
        }
    }

    /**
     * Represents multiple intents that can be processed in parallel.
     * Uses SomeOf to allow optional command and/or query.
     */
    public record MultipleIntents(
            UserIntent.Command command,
            UserIntent.Query query
    ) implements SomeOf {}

    /**
     * Handles multiple intents using ScatterGatherBuilder for parallel execution.
     */
    @State
    public record MultiIntentState(
            MultipleIntents intents,
            CommandOrchestrator commandOrchestrator,
            QueryAgent queryAgent
    ) implements IntentState {
        @Action
        public PreFormatState processMultipleIntents(ActionContext context) {
            List<Supplier<AgentMessageResponse>> tasks = new ArrayList<>();

            // Add command processing task if present
            if (intents.command() != null) {
                final UserIntent.Command cmd = intents.command();
                tasks.add(() -> RunSubagent
                        .fromAnnotatedInstance(commandOrchestrator, CommandOrchestrator.CommandOrchestratorResponse.class));
//                        .asSubProcess(context, cmd));
            }

            // Add query processing task if present
            if (intents.query() != null) {
                final UserIntent.Query qry = intents.query();
                tasks.add(() -> RunSubagent
                        .fromAnnotatedInstance(queryAgent, QueryAgent.QuerySubagentResponse.class));
//                        .asSubProcess(context, qry));
            }

            // Use ScatterGatherBuilder for parallel execution
            String combinedMessage = ScatterGatherBuilder
                    .returning(String.class)
                    .fromElements(AgentMessageResponse.class)
                    .generatedBy(tasks)
                    .consolidatedBy(ctx -> {
                        List<String> messages = new ArrayList<>();
                        for (AgentMessageResponse response : ctx.getInput().getResults()) {
                            messages.add(response.message());
                        }
                        return String.join("\n\n", messages);
                    })
                    .asSubProcess(context);

            return new PreFormatState(combinedMessage);
        }
    }

    /**
     * Handles single commands by delegating to CommandOrchestrator.
     */
    @State
    public record CommandState(
            UserIntent.Command command,
            CommandOrchestrator commandOrchestrator
    ) implements IntentState {
        @Action
        public PreFormatState processCommand(ActionContext context) {
            CommandOrchestrator.CommandOrchestratorResponse response = RunSubagent
                    .fromAnnotatedInstance(commandOrchestrator, CommandOrchestrator.CommandOrchestratorResponse.class);
//                    .asSubProcess(context, command);
            return new PreFormatState(response.message());
        }
    }

    /**
     * Pre-formatting state - can be used to translate or format the message.
     * Currently translates to Portuguese as in the original implementation.
     */
    @State
    public record PreFormatState(String message) implements IntentState {
        @Action
        public FinalState formatResponse(Ai ai) {
            String formatted = ai.withAutoLlm()
                    .withId("translate-to-portuguese")
                    .generateText("""
                            Translate the following response into Portuguese.
                            Keep the same tone and style, but make it natural Portuguese.
                            If there's ASCII art, keep it intact.

                            Original response:
                            %s""".formatted(message));
            return new FinalState(formatted);
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
}
