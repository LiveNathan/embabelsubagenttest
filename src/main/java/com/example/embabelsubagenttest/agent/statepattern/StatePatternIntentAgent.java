package com.example.embabelsubagenttest.agent.statepattern;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.State;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.SomeOf;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Agent(description = "Routes user requests to the appropriate specialist agent")
public class StatePatternIntentAgent {

    private final AgentPlatform agentPlatform;

    public StatePatternIntentAgent(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    @Action
    public IntentState classifyAndRoute(UserInput userInput, Ai ai) {
        UserIntent intent = ai.withAutoLlm()
                .creating(UserIntent.class)
                .fromPrompt(createClassifyIntentPrompt(userInput));

        return switch (intent) {
            case UserIntent.Query query -> new QueryState(query);
            case UserIntent.Command command -> new CommandState(command, agentPlatform);
            case UserIntent.Unknown unknown -> new UnknownState(unknown);
            case UserIntent.Multiple multiple -> new MultiIntentState(
                    new MultipleIntents(
                            new UserIntent.Command(multiple.commandDescription()),
                            new UserIntent.Query(multiple.queryQuestion())
                    ),
                    agentPlatform
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
    public sealed interface IntentState {
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CommandType.BananaArt.class, name = "BANANA_ART"),
            @JsonSubTypes.Type(value = CommandType.FortuneCookie.class, name = "FORTUNE_COOKIE"),
            @JsonSubTypes.Type(value = CommandType.DadJoke.class, name = "DAD_JOKE"),
            @JsonSubTypes.Type(value = CommandType.Multiple.class, name = "MULTIPLE"),
            @JsonSubTypes.Type(value = CommandType.Unknown.class, name = "UNKNOWN")
    })
    public sealed interface CommandType {
        record BananaArt() implements CommandType {
        }

        record FortuneCookie() implements CommandType {
        }

        record DadJoke() implements CommandType {
        }

        record Multiple(boolean wantsBanana, boolean wantsFortune, boolean wantsJoke) implements CommandType {
        }

        record Unknown(String reason) implements CommandType {
        }
    }

    public record IntentAgentResponse(String message) {
    }

    @State
    public record QueryState(UserIntent.Query query) implements IntentState {
        @Action
        public PreTranslationState processQuery(Ai ai) {
            StatePatternQueryAgent.QuerySubagentResponse response = ai.withAutoLlm()
                    .withId("respond-to-query")
                    .creating(StatePatternQueryAgent.QuerySubagentResponse.class)
                    .fromPrompt("""
                            You are a helpful assistant. Answer the user's question.
                            
                            User question: %s""".formatted(query.question()));
            return new PreTranslationState(response.message());
        }
    }

    @State
    public record UnknownState(UserIntent.Unknown unknown) implements IntentState {
        @Action
        public PreTranslationState handleUnknown() {
            return new PreTranslationState("I'm not sure what you're asking for: " + unknown.reason());
        }
    }

    /**
     * Represents multiple intents that can be processed in parallel.
     * Uses SomeOf to allow optional command and/or query.
     * All non-null fields will be bound to the blackboard.
     */
    public record MultipleIntents(
            UserIntent.Command command,
            UserIntent.Query query
    ) implements SomeOf {
    }

    @State
    public record PreTranslationState(String message) implements IntentState {
        @Action
        public FinalState translate(Ai ai) {
            String translated = ai.withAutoLlm()
                    .withId("translate-to-portuguese")
                    .generateText("""
                            Translate the following response into Portuguese.
                            Keep the same tone and style, but make it natural Portuguese.
                            If there's ASCII art, keep it intact.
                            
                            Original response:
                            %s""".formatted(message));
            return new FinalState(translated);
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

    /**
     * Handles multiple intents by executing them in parallel using CompletableFuture.
     * This pattern can be reused for any scenario requiring parallel agent execution.
     */
    @State
    public record MultiIntentState(MultipleIntents intents, AgentPlatform agentPlatform) implements IntentState {
        @Action
        public PreTranslationState processMultipleIntents(Ai ai) {
            List<CompletableFuture<String>> futures = new ArrayList<>();

            // Add command processing task if present
            if (intents.command() != null) {
                futures.add(CompletableFuture.supplyAsync(() ->
                        processCommand(intents.command(), ai)
                ));
            }

            // Add query processing task if present
            if (intents.query() != null) {
                futures.add(CompletableFuture.supplyAsync(() ->
                        processQuery(intents.query(), ai)
                ));
            }

            // Wait for all tasks to complete and combine results
            String combinedMessage = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.joining("\n\n"));

            return new PreTranslationState(combinedMessage);
        }

        private String processCommand(UserIntent.Command command, Ai ai) {
            // Classify and route the command
            CommandType commandType = ai.withAutoLlm()
                    .creating(CommandType.class)
                    .fromPrompt("""
                            Classify the user's command into one of these categories:
                            - BANANA_ART: User wants ONLY to see ASCII art of bananas
                            - FORTUNE_COOKIE: User wants ONLY a fortune cookie message or inspirational quote
                            - DAD_JOKE: User wants ONLY to hear a joke
                            - MULTIPLE: User wants MORE THAN ONE of the above
                            - UNKNOWN: Command doesn't match any of the above
                            
                            User command: %s
                            
                            Return the appropriate type.""".formatted(command.description()));

            return switch (commandType) {
                case CommandType.BananaArt ignored -> invokeStatePatternBananaArtAgent(command);
                case CommandType.FortuneCookie ignored -> invokeStatePatternFortuneCookieAgent(command);
                case CommandType.DadJoke ignored -> invokeStatePatternDadJokeAgent(command);
                case CommandType.Multiple multiple -> processMultipleCommandsInternal(multiple, command);
                case CommandType.Unknown unknown -> "Sorry, I don't understand that command: " + unknown.reason();
            };
        }

        private String processMultipleCommandsInternal(CommandType.Multiple multiple, UserIntent.Command command) {
            List<CompletableFuture<String>> futures = new ArrayList<>();

            if (multiple.wantsBanana()) {
                futures.add(CompletableFuture.supplyAsync(() -> invokeStatePatternBananaArtAgent(command)));
            }
            if (multiple.wantsFortune()) {
                futures.add(CompletableFuture.supplyAsync(() -> invokeStatePatternFortuneCookieAgent(command)));
            }
            if (multiple.wantsJoke()) {
                futures.add(CompletableFuture.supplyAsync(() -> invokeStatePatternDadJokeAgent(command)));
            }

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.joining("\n\n"));
        }

        private String processQuery(UserIntent.Query query, Ai ai) {
            StatePatternQueryAgent.QuerySubagentResponse response = ai.withAutoLlm()
                    .withId("respond-to-query")
                    .creating(StatePatternQueryAgent.QuerySubagentResponse.class)
                    .fromPrompt("""
                            You are a helpful assistant. Answer the user's question.
                            
                            User question: %s""".formatted(query.question()));
            return response.message();
        }

        private String invokeStatePatternBananaArtAgent(UserIntent.Command command) {
            StatePatternBananaArtAgent.ArtResponse response = AgentInvocation
                    .create(agentPlatform, StatePatternBananaArtAgent.ArtResponse.class)
                    .invoke(new StatePatternBananaArtAgent.ArtRequest(command.description()));
            return response.message();
        }

        private String invokeStatePatternFortuneCookieAgent(UserIntent.Command command) {
            StatePatternFortuneCookieAgent.FortuneResponse response = AgentInvocation
                    .create(agentPlatform, StatePatternFortuneCookieAgent.FortuneResponse.class)
                    .invoke(new StatePatternFortuneCookieAgent.FortuneRequest(command.description()));
            return response.message();
        }

        private String invokeStatePatternDadJokeAgent(UserIntent.Command command) {
            StatePatternDadJokeAgent.JokeResponse response = AgentInvocation
                    .create(agentPlatform, StatePatternDadJokeAgent.JokeResponse.class)
                    .invoke(new StatePatternDadJokeAgent.JokeRequest(command.description()));
            return response.message();
        }
    }

    @State
    public record CommandState(UserIntent.Command command, AgentPlatform agentPlatform) implements IntentState {
        @Action
        public PreTranslationState processCommand(Ai ai) {
            CommandType commandType = ai.withAutoLlm()
                    .creating(CommandType.class)
                    .fromPrompt("""
                            Classify the user's command into one of these categories:
                            - BANANA_ART: User wants ONLY to see ASCII art of bananas
                            - FORTUNE_COOKIE: User wants ONLY a fortune cookie message or inspirational quote
                            - DAD_JOKE: User wants ONLY to hear a joke
                            - MULTIPLE: User wants MORE THAN ONE of the above (e.g., "show banana and tell joke")
                            - UNKNOWN: Command doesn't match any of the above
                            
                            Examples:
                            - "Show me a banana" → BANANA_ART
                            - "Tell me a joke" → DAD_JOKE
                            - "Show me a banana and tell me a joke" → MULTIPLE (wantsBanana=true, wantsJoke=true)
                            - "Give me a fortune and a joke" → MULTIPLE (wantsFortune=true, wantsJoke=true)
                            - "Show banana, tell joke, give fortune" → MULTIPLE (all true)
                            
                            User command: %s
                            
                            Return the appropriate type.""".formatted(command.description()));

            String message = switch (commandType) {
                case CommandType.BananaArt ignored -> invokeStatePatternBananaArtAgent();
                case CommandType.FortuneCookie ignored -> invokeStatePatternFortuneCookieAgent();
                case CommandType.DadJoke ignored -> invokeStatePatternDadJokeAgent();
                case CommandType.Multiple multiple -> processMultipleCommands(multiple);
                case CommandType.Unknown unknown -> "Sorry, I don't understand that command: " + unknown.reason();
            };

            return new PreTranslationState(message);
        }

        /**
         * Processes multiple commands in parallel using CompletableFuture.
         * This allows requests like "show me a banana and tell me a joke" to execute concurrently.
         */
        private String processMultipleCommands(CommandType.Multiple multiple) {
            List<CompletableFuture<String>> futures = new ArrayList<>();

            if (multiple.wantsBanana()) {
                futures.add(CompletableFuture.supplyAsync(this::invokeStatePatternBananaArtAgent));
            }
            if (multiple.wantsFortune()) {
                futures.add(CompletableFuture.supplyAsync(this::invokeStatePatternFortuneCookieAgent));
            }
            if (multiple.wantsJoke()) {
                futures.add(CompletableFuture.supplyAsync(this::invokeStatePatternDadJokeAgent));
            }

            // Wait for all commands to complete and combine results
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.joining("\n\n"));
        }

        private String invokeStatePatternBananaArtAgent() {
            StatePatternBananaArtAgent.ArtResponse response = AgentInvocation
                    .create(agentPlatform, StatePatternBananaArtAgent.ArtResponse.class)
                    .invoke(new StatePatternBananaArtAgent.ArtRequest(command.description()));
            return response.message();
        }

        private String invokeStatePatternFortuneCookieAgent() {
            StatePatternFortuneCookieAgent.FortuneResponse response = AgentInvocation
                    .create(agentPlatform, StatePatternFortuneCookieAgent.FortuneResponse.class)
                    .invoke(new StatePatternFortuneCookieAgent.FortuneRequest(command.description()));
            return response.message();
        }

        private String invokeStatePatternDadJokeAgent() {
            StatePatternDadJokeAgent.JokeResponse response = AgentInvocation
                    .create(agentPlatform, StatePatternDadJokeAgent.JokeResponse.class)
                    .invoke(new StatePatternDadJokeAgent.JokeRequest(command.description()));
            return response.message();
        }
    }
}
