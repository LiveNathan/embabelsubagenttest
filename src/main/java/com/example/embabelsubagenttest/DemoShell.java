package com.example.embabelsubagenttest;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import com.example.embabelsubagenttest.agent.hierarchical.HierarchicalIntentAgent;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedIntentAgent;
import com.example.embabelsubagenttest.agent.scattergather.ScatterGatherIntentAgent;
import com.example.embabelsubagenttest.agent.statepattern.StatePatternIntentAgent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
record DemoShell(AgentPlatform agentPlatform) {

    @ShellMethod("Hierarchical Intent (Main)")
    String intentHierarchical(final String content) {
        HierarchicalIntentAgent.IntentAgentResponse response = AgentInvocation
                .create(agentPlatform, HierarchicalIntentAgent.IntentAgentResponse.class)
                .invoke(new UserInput(content));
        return response.message();
    }

    @ShellMethod("State Pattern Intent")
    String intentStatePattern(final String content) {
        StatePatternIntentAgent.IntentAgentResponse response = AgentInvocation
                .create(agentPlatform, StatePatternIntentAgent.IntentAgentResponse.class)
                .invoke(new UserInput(content));
        return response.message();
    }

    @ShellMethod("Scatter Gather Intent (Parallel GOAP)")
    String intentScatterGather(final String content) {
        ScatterGatherIntentAgent.IntentAgentResponse response = AgentInvocation
                .create(agentPlatform, ScatterGatherIntentAgent.IntentAgentResponse.class)
                .invoke(new UserInput(content));
        return response.message();
    }

    @ShellMethod("Orchestrated Intent (Refactored)")
    String intentOrchestrated(final String content) {
        OrchestratedIntentAgent.FinalResponse response = AgentInvocation
                .create(agentPlatform, OrchestratedIntentAgent.FinalResponse.class)
                .invoke(new UserInput(content));
        return response.message();
    }
}