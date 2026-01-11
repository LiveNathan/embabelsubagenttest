package com.example.embabelsubagenttest.agent.hierarchical;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.RunSubagent;
import com.embabel.agent.api.common.Ai;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Agent(description = "Routes commands to specialized command handlers")
public class HierarchicalCommandAgent {
    private final HierarchicalBananaArtAgent bananaArtAgent;
    private final HierarchicalFortuneCookieAgent fortuneCookieAgent;
    private final HierarchicalDadJokeAgent dadJokeAgent;

    public HierarchicalCommandAgent(HierarchicalBananaArtAgent bananaArtAgent, HierarchicalFortuneCookieAgent fortuneCookieAgent, HierarchicalDadJokeAgent dadJokeAgent) {
        this.bananaArtAgent = bananaArtAgent;
        this.fortuneCookieAgent = fortuneCookieAgent;
        this.dadJokeAgent = dadJokeAgent;
    }

    @Action
    public CommandIntent executeCommand(HierarchicalIntentAgent.UserIntent.Command command, Ai ai) {
        return ai.withAutoLlm()
                .creating(CommandIntent.class)
                .fromPrompt(createClassifyCommandPrompt(command));
    }

    String createClassifyCommandPrompt(HierarchicalIntentAgent.UserIntent.Command command) {
        return String.format("""
                        Classify the user's command into one of these categories:
                        - BANANA_ART: User wants to see ASCII art of bananas
                        - FORTUNE_COOKIE: User wants a fortune cookie message or inspirational quote
                        - DAD_JOKE: User wants to hear a joke
                        - UNKNOWN: Command doesn't match any of the above
                        
                        User command: %s
                        
                        Return the appropriate type with a description or reason.""",
                command.description()).trim();
    }

    @Action
    public CommandSubagentResponse handleCommand(CommandIntent intent) {
        return switch (intent) {
            case CommandIntent.BananaArt bananaArt ->
                    RunSubagent.fromAnnotatedInstance(bananaArtAgent, CommandSubagentResponse.class);
            case CommandIntent.FortuneCookie fortuneCookie ->
                    RunSubagent.fromAnnotatedInstance(fortuneCookieAgent, CommandSubagentResponse.class);
            case CommandIntent.DadJoke dadJoke ->
                    RunSubagent.fromAnnotatedInstance(dadJokeAgent, CommandSubagentResponse.class);
            case CommandIntent.Unknown unknown ->
                    new UnknownCommandResponse("Sorry, I don't understand that command: " + unknown.reason());
        };
    }

    @AchievesGoal(description = "User command is executed")
    @Action
    public CommandAgentResponse done(CommandSubagentResponse response) {
        return new CommandAgentResponse(response.message());
    }

    public interface CommandSubagentResponse {
        String message();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CommandIntent.BananaArt.class, name = "BANANA_ART"),
            @JsonSubTypes.Type(value = CommandIntent.FortuneCookie.class, name = "FORTUNE_COOKIE"),
            @JsonSubTypes.Type(value = CommandIntent.DadJoke.class, name = "DAD_JOKE"),
            @JsonSubTypes.Type(value = CommandIntent.Unknown.class, name = "UNKNOWN")
    })
    public sealed interface CommandIntent {
        record BananaArt(String description) implements CommandIntent {
        }

        record FortuneCookie(String description) implements CommandIntent {
        }

        record DadJoke(String description) implements CommandIntent {
        }

        record Unknown(String reason) implements CommandIntent {
        }
    }

    public record CommandAgentResponse(String message) implements AgentMessageResponse {
    }

    public record UnknownCommandResponse(String message) implements CommandSubagentResponse {
    }
}
