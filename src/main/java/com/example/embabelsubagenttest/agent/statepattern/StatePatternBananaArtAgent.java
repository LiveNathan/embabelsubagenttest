package com.example.embabelsubagenttest.agent.statepattern;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Agent(description = "Generates ASCII art of fruits with various styles and sizes")
public class StatePatternBananaArtAgent {

    @AchievesGoal(description = "ASCII art generated")
    @Action
    public ArtResponse generateArt(ArtRequest request, Ai ai) {
        // Classify the style preference
        ArtStyle style = ai.withAutoLlm()
                .withId("classify-art-style")
                .creating(ArtStyle.class)
                .fromPrompt("""
                        Classify the user's art style preference:
                        - CLASSIC: Traditional detailed ASCII art (default if not specified)
                        - SIMPLE: Minimalist, small ASCII art
                        - DETAILED: Complex, large ASCII art with fine details
                        
                        User request: %s
                        
                        Return the appropriate style.""".formatted(request.description()));

        String art = switch (style) {
            case ArtStyle.Classic ignored -> generateClassicBanana();
            case ArtStyle.Simple ignored -> generateSimpleBanana();
            case ArtStyle.Detailed ignored -> generateDetailedBanana();
        };

        return new ArtResponse(art);
    }

    private String generateClassicBanana() {
        return """
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
                             `-.__  `----""\\"    __.-'
                hh                `--..____..--'
                """;
    }

    private String generateSimpleBanana() {
        return """
                  ___
                 _)_)_
                (______)
                """;
    }

    private String generateDetailedBanana() {
        return """
                          _.._
                        .'    '.
                       /   __   \\
                      |  ,'  '.  |
                      | /      \\ |
                      |/        \\|
                     _||        ||_
                   ,'  |        |  '.
                  /    |        |    \\
                 /     |        |     \\
                |      |        |      |
                |      |        |      |
                |      |        |      |
                |      |        |      |
                 \\     |        |     /
                  \\    |        |    /
                   '.  |        |  .'
                     '-|        |-'
                       |        |
                       |        |
                       |________|
                       (__/  \\__)
                """;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ArtStyle.Classic.class, name = "CLASSIC"),
            @JsonSubTypes.Type(value = ArtStyle.Simple.class, name = "SIMPLE"),
            @JsonSubTypes.Type(value = ArtStyle.Detailed.class, name = "DETAILED")
    })
    public sealed interface ArtStyle {
        record Classic() implements ArtStyle {
        }

        record Simple() implements ArtStyle {
        }

        record Detailed() implements ArtStyle {
        }
    }

    public record ArtRequest(String description) {
    }

    public record ArtResponse(String art) implements AgentMessageResponse {
        @Override
        public String message() {
            return art;
        }
    }
}
