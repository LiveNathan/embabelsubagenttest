package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;

@Agent(description = "Creates ASCII art of bananas")
public class BananaArtAgent {
    public record BananaArtResponse(String message) implements CommandAgent.CommandSubagentResponse {
    }

    @AchievesGoal(description = "ASCII art created")
    @Action
    public BananaArtResponse createBananaArt(CommandAgent.CommandIntent.BananaArt request) {
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
        return new BananaArtResponse(art);
    }
}
