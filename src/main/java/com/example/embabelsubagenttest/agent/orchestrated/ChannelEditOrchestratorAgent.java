package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.Agent;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedIntentAgent.OrchestratedResponse;

/**
 * Sub-agent responsible for orchestrating complex channel edits.
 */
@Agent(description = "Specialist agent for performing channel name, color, and routing edits")
public class ChannelEditOrchestratorAgent {

    public ChannelEditOrchestratorAgent() {
    }

    // This agent will eventually have actions to handle OrchestratedIntentAgent.UserIntent.Command
    // and produce OrchestratedResponse.
}
