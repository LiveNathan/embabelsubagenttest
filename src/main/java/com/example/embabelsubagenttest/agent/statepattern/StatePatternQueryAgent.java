package com.example.embabelsubagenttest.agent.statepattern;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Respond to query")
public class StatePatternQueryAgent {
    @AchievesGoal(description = "User question is answered.")
    @Action
    public QuerySubagentResponse answerUserQuestion(StatePatternIntentAgent.UserIntent.Query query, Ai ai) {
        return ai.withAutoLlm()
                .withId("respond-to-query")
                .creating(QuerySubagentResponse.class)
                .fromPrompt("""
                        You are a helpful assistant. Answer the user's question.
                        
                        User question: %s""".formatted(query.question()));
    }

    public record QuerySubagentResponse(String message) implements AgentMessageResponse {
    }
}
