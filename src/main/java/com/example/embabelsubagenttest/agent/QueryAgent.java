package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Respond to query")
public class QueryAgent {
    public record QuerySubagentResponse(String message) implements AgentMessageResponse {
    }

    @AchievesGoal(description = "User question is answered.")
    @Action
    public QuerySubagentResponse answerUserQuestion(IntentAgent.UserIntent.Query query, Ai ai) {
        return ai.withAutoLlm()
                .withId("respond-to-query")
                .creating(QuerySubagentResponse.class)
                .fromPrompt("""
                        You are a helpful assistant. Answer the user's question.

                        User question: %s""".formatted(query.question()));
    }
}
