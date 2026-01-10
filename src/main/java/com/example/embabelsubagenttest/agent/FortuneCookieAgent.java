package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Generates fortune cookie messages")
public class FortuneCookieAgent {
    public record FortuneCookieResponse(String message) implements CommandAgent.CommandSubagentResponse {
    }

    @AchievesGoal(description = "Fortune cookie generated")
    @Action
    public FortuneCookieResponse generateFortune(CommandAgent.CommandIntent.FortuneCookie request, Ai ai) {
        FortuneCookieResponse response = ai.withAutoLlm()
                .withId("generate-fortune")
                .creating(FortuneCookieResponse.class)
                .fromPrompt("""
                        Generate a creative and inspiring fortune cookie message.
                        Make it wise, optimistic, and slightly mysterious.
                        Keep it under 30 words.
                        """);
        return response;
    }
}
