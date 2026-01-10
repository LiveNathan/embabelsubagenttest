package com.example.embabelsubagenttest;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import com.example.embabelsubagenttest.agent.IntentAgent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
record DemoShell(AgentPlatform agentPlatform) {

    @ShellMethod("Intent")
    String intent(final String content) {
        IntentAgent.IntentAgentResponse intentAgentResponse = AgentInvocation
                .create(agentPlatform, IntentAgent.IntentAgentResponse.class)
                .invoke(new UserInput(content));
        return intentAgentResponse.message();
    }
}
