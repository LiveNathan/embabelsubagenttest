package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.common.SomeOf;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.BananaArtRequest;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.BananaArtResult;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.FortuneRequest;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.FortuneResult;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.JokeRequest;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.JokeResult;
import org.springframework.lang.Nullable;

/**
 * Domain types for the orchestrated agent pattern.
 * Mirrors the structure of CommandTypes but tailored for the orchestrated package.
 */
public class OrchestratedTypes {

    /**
     * OrchestratedRequest implements SomeOf - LLM populates applicable fields.
     * This allows the orchestrator to determine which specialized tasks to perform.
     */
    public record OrchestratedRequest(
            @Nullable BananaArtRequest bananaArt,
            @Nullable FortuneRequest fortune,
            @Nullable JokeRequest joke
    ) implements SomeOf {
        public boolean isEmpty() {
            return bananaArt == null && fortune == null && joke == null;
        }
    }

    /**
     * Aggregated results from the orchestration flow.
     */
    public record OrchestratedResults(
            @Nullable BananaArtResult bananaArt,
            @Nullable FortuneResult fortune,
            @Nullable JokeResult joke
    ) {
    }
}
