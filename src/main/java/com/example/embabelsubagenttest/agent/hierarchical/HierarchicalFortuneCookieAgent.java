package com.example.embabelsubagenttest.agent.hierarchical;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Generates fortune cookie messages")
public class HierarchicalFortuneCookieAgent {
    @AchievesGoal(description = "Fortune cookie message generated")
    @Action
    public FortuneResponse generateFortune(HierarchicalCommandAgent.CommandIntent.FortuneCookie request, Ai ai) {
        return ai.withAutoLlm()
                .withId("generate-fortune")
                .creating(FortuneResponse.class)
                .fromPrompt(String.format("""
                        Generate a creative and inspiring fortune cookie message based on the user's description.
                        Make it wise, optimistic, and slightly mysterious.
                        Keep it under 30 words.
                        
                        User description: %s
                        """, request.description()));
    }

    public record FortuneResponse(String message) implements HierarchicalCommandAgent.CommandSubagentResponse {
    }
}
