package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.State;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Agent(description = "Routes user requests to the appropriate specialist agent")
public class IntentAgent {

    private final AgentPlatform agentPlatform;

    public IntentAgent(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    public record IntentAgentResponse(String message) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "intent")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserIntent.Command.class, name = "COMMAND"),
            @JsonSubTypes.Type(value = UserIntent.Query.class, name = "QUERY"),
            @JsonSubTypes.Type(value = UserIntent.Unknown.class, name = "UNKNOWN")
    })
    public sealed interface UserIntent {
        record Command(String description) implements UserIntent {
        }

        record Query(String question) implements UserIntent {
        }

        record Unknown(String reason) implements UserIntent {
        }
    }

    @State
    public sealed interface IntentState {
    }

    @State
    public record QueryState(UserIntent.Query query) implements IntentState {
        @Action
        public PreTranslationState processQuery(Ai ai) {
            QueryAgent.QuerySubagentResponse response = ai.withAutoLlm()
                    .withId("respond-to-query")
                    .creating(QueryAgent.QuerySubagentResponse.class)
                    .fromPrompt("""
                            You are a helpful assistant. Answer the user's question.

                            User question: %s""".formatted(query.question()));
            return new PreTranslationState(response.message());
        }
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
        };
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CommandType.BananaArt.class, name = "BANANA_ART"),
            @JsonSubTypes.Type(value = CommandType.FortuneCookie.class, name = "FORTUNE_COOKIE"),
            @JsonSubTypes.Type(value = CommandType.DadJoke.class, name = "DAD_JOKE"),
            @JsonSubTypes.Type(value = CommandType.Unknown.class, name = "UNKNOWN")
    })
    public sealed interface CommandType {
        record BananaArt() implements CommandType {
        }

        record FortuneCookie() implements CommandType {
        }

        record DadJoke() implements CommandType {
        }

        record Unknown(String reason) implements CommandType {
        }
    }

    @State
    public record UnknownState(UserIntent.Unknown unknown) implements IntentState {
        @Action
        public PreTranslationState handleUnknown() {
            return new PreTranslationState("I'm not sure what you're asking for: " + unknown.reason());
        }
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

    @State
    public record CommandState(UserIntent.Command command, AgentPlatform agentPlatform) implements IntentState {
        @Action
        public PreTranslationState processCommand(Ai ai) {
            CommandType commandType = ai.withAutoLlm()
                    .creating(CommandType.class)
                    .fromPrompt("""
                            Classify the user's command into one of these categories:
                            - BANANA_ART: User wants to see ASCII art of bananas
                            - FORTUNE_COOKIE: User wants a fortune cookie message or inspirational quote
                            - DAD_JOKE: User wants to hear a joke
                            - UNKNOWN: Command doesn't match any of the above

                            User command: %s

                            Return the appropriate type.""".formatted(command.description()));

            String message = switch (commandType) {
                case CommandType.BananaArt ignored -> invokeBananaArtAgent();
                case CommandType.FortuneCookie ignored -> invokeFortuneCookieAgent();
                case CommandType.DadJoke ignored -> invokeDadJokeAgent();
                case CommandType.Unknown unknown ->
                        "Sorry, I don't understand that command: " + unknown.reason();
            };

            return new PreTranslationState(message);
        }

        private String invokeBananaArtAgent() {
            BananaArtAgent.ArtResponse response = AgentInvocation
                    .create(agentPlatform, BananaArtAgent.ArtResponse.class)
                    .invoke(new BananaArtAgent.ArtRequest(command.description()));
            return response.message();
        }

        private String invokeFortuneCookieAgent() {
            FortuneCookieAgent.FortuneResponse response = AgentInvocation
                    .create(agentPlatform, FortuneCookieAgent.FortuneResponse.class)
                    .invoke(new FortuneCookieAgent.FortuneRequest(command.description()));
            return response.message();
        }

        private String invokeDadJokeAgent() {
            DadJokeAgent.JokeResponse response = AgentInvocation
                    .create(agentPlatform, DadJokeAgent.JokeResponse.class)
                    .invoke(new DadJokeAgent.JokeRequest(command.description()));
            return response.message();
        }
    }

    String createClassifyIntentPrompt(UserInput userInput) {
        return String.format("""
                        Classify the user's intent:
                        - COMMAND: User wants to see banana ASCII art, get a fortune cookie message, or hear a dad joke
                        - QUERY: User is asking a general question or requesting information
                        - UNKNOWN: User's intent is unclear or doesn't match the above categories

                        User message: %s

                        Return Command with a clear description of what they want (banana art, fortune, or joke), Query with the question they're asking, or Unknown with the reason.""",
                userInput.getContent()).trim();
    }
}
