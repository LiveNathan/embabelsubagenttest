package com.example.embabelsubagenttest.service;

import com.embabel.agent.api.common.Ai;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.BananaArtRequest;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.BananaArtResult;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.stereotype.Component;

/**
 * Service for generating ASCII art of bananas.
 * Plain Spring Component (not an @Agent) - designed to be called from CommandOrchestrator.
 */
@Component
public class BananaArtService {

    public BananaArtResult generate(BananaArtRequest request, Ai ai) {
        try {
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

            return BananaArtResult.success(art);

        } catch (Exception e) {
            return BananaArtResult.error("Failed to generate banana art: " + e.getMessage());
        }
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
        record Classic() implements ArtStyle {}
        record Simple() implements ArtStyle {}
        record Detailed() implements ArtStyle {}
    }
}
