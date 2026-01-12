package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelsubagenttest.agent.AgentMessageResponse;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedIntentAgent.UserIntent;

@Agent(description = "Respond to general queries")
public class OrchestratedQueryAgent {

    @AchievesGoal(description = "User question is answered.")
    @Action
    public QueryResponse answerUserQuestion(UserIntent.Query query, Ai ai) {
        String answer = ai.withAutoLlm()
                .withId("orchestrated-query")
                .generateText("""
                        You are a helpful assistant. Answer the user's question clearly and concisely.
                        
                        User question: %s""".formatted(query.question()));
        
        return new QueryResponse(answer);
    }

    public record QueryResponse(String answer) implements AgentMessageResponse {
        @Override
        public String message() {
            return answer;
        }
    }
}
