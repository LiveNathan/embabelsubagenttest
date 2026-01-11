package com.example.embabelsubagenttest.agent.hierarchical;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;

@Agent(description = "Creates ASCII art of bananas")
public class HierarchicalBananaArtAgent {
    @AchievesGoal(description = "ASCII art created")
    @Action
    public BananaArtResponse createBananaArt(HierarchicalCommandAgent.CommandIntent.BananaArt request) {
        String art = String.format("""
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
                request: %s""", request);
        return new BananaArtResponse(art);
    }

    public record BananaArtResponse(String message) implements HierarchicalCommandAgent.CommandSubagentResponse {
    }
}
