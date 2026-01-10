package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.Ai;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.beans.factory.annotation.Autowired;

@EmbabelComponent
public class CommandAgent {

    public record CommandAgentResponse(String message) implements AgentMessageResponse {
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

    /**
     * Entry point for handling commands from IntentAgent.
     * Classifies the command type and delegates to the appropriate handler.
     */
    public CommandAgentResponse handle(IntentAgent.UserIntent.Command command, Ai ai) {
        // Classify the command type
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

        // Route to the appropriate handler
        return switch (commandType) {
            case CommandType.BananaArt ignored -> generateBananaArt();
            case CommandType.FortuneCookie ignored -> generateFortune(ai);
            case CommandType.DadJoke ignored -> tellJoke(ai);
            case CommandType.Unknown unknown ->
                    new CommandAgentResponse("Sorry, I don't understand that command: " + unknown.reason());
        };
    }

    private CommandAgentResponse generateBananaArt() {
        String art = """
                 _
                //\\
                V  \\
                 \\  \\_
                  \\,'.`-.
                   |\\ `. `.
                   ( \\  `. `-.                        _,.-:\\
                    \\ \\   `.  `-._             __..--' ,-';/
                     \\ `.   `-.   `-..___..---'   _.--' ,'/
                      `. `.    `-._        __..--'    ,' /
                        `. `-_     ``--..''       _.-' ,'
                          `-_ `-.___        __,--'   ,'
                             `-.__  `----""\"    __.-'
                hh                `--..____..--'
                """;
        return new CommandAgentResponse(art);
    }

    private CommandAgentResponse generateFortune(Ai ai) {
        return ai.withAutoLlm()
                .withId("generate-fortune")
                .creating(CommandAgentResponse.class)
                .fromPrompt("""
                        Generate a creative and inspiring fortune cookie message.
                        Make it wise, optimistic, and slightly mysterious.
                        Keep it under 30 words.
                        """);
    }

    private CommandAgentResponse tellJoke(Ai ai) {
        return ai.withAutoLlm()
                .withId("tell-dad-joke")
                .creating(CommandAgentResponse.class)
                .fromPrompt("""
                        Tell a classic dad joke about programming or technology.
                        Make it wholesome and groan-worthy.
                        Include both the setup and punchline.
                        """);
    }
}
