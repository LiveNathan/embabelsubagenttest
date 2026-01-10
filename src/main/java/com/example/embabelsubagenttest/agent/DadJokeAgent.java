package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Tells dad jokes")
public class DadJokeAgent {
    public record DadJokeResponse(String message) implements CommandAgent.CommandSubagentResponse {
    }

    @AchievesGoal(description = "Dad joke delivered")
    @Action
    public DadJokeResponse tellJoke(CommandAgent.CommandIntent.DadJoke request, Ai ai) {
        return ai.withAutoLlm()
                .withId("tell-dad-joke")
                .creating(DadJokeResponse.class)
                .fromPrompt("""
                        Tell a classic dad joke about programming or technology.
                        Make it wholesome and groan-worthy.
                        Include both the setup and punchline.
                        """);
    }
}
