package com.example.embabelsubagenttest.agent.hierarchical;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Tells dad jokes")
public class HierarchicalDadJokeAgent {
    @AchievesGoal(description = "Dad joke told")
    @Action
    public JokeResponse tellJoke(HierarchicalCommandAgent.CommandIntent.DadJoke request, Ai ai) {
        return ai.withAutoLlm()
                .withId("tell-dad-joke")
                .creating(JokeResponse.class)
                .fromPrompt(String.format("""
                        Tell a classic dad joke about the topic: %s
                        Make it wholesome and groan-worthy.
                        Include both the setup and punchline.
                        """, request.description()));
    }

    public record JokeResponse(String message) implements HierarchicalCommandAgent.CommandSubagentResponse {
    }
}
