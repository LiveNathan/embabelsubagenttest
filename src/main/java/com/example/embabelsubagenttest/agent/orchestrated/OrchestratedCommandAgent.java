package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.OrchestratedRequest;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.OrchestratedResults;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.BananaArtResult;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.FortuneResult;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.JokeResult;
import com.example.embabelsubagenttest.service.BananaArtService;
import com.example.embabelsubagenttest.service.FortuneService;
import com.example.embabelsubagenttest.service.JokeService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Agent that orchestrates command execution by classifying intent into multiple parallel tasks.
 * Uses CompletableFuture for parallel execution and summarizes results into a single response.
 */
@Agent(description = "Orchestrates commands by delegating to specialized services in parallel")
public class OrchestratedCommandAgent {

    private final BananaArtService bananaArtService;
    private final FortuneService fortuneService;
    private final JokeService jokeService;

    public OrchestratedCommandAgent(
            BananaArtService bananaArtService,
            FortuneService fortuneService,
            JokeService jokeService) {
        this.bananaArtService = bananaArtService;
        this.fortuneService = fortuneService;
        this.jokeService = jokeService;
    }

    /**
     * Handles a user command by:
     * 1. Classifying the request into one or more service calls using OrchestratedRequest (SomeOf).
     * 2. Executing applicable services in parallel using CompletableFuture.
     * 3. Summarizing the output.
     */
    @AchievesGoal(description = "Command processed and results summarized")
    @Action
    public OrchestratedResponse handleCommand(String command, ActionContext context) {
        // Step 1: Classify command using LLM
        OrchestratedRequest request = context.ai().withAutoLlm()
                .withId("classify-orchestrated-request")
                .creating(OrchestratedRequest.class)
                .fromPrompt("""
                        Analyze the user's command and determine which services should be invoked.
                        You can populate one or more of the following fields:

                        - bananaArt: If the user wants ASCII art of a banana
                        - fortune: If the user wants a fortune cookie message
                        - joke: If the user wants a dad joke

                        User command: %s

                        Provide a description for each applicable service. Leave others null.""".formatted(command));

        if (request.isEmpty()) {
            return new OrchestratedResponse("I didn't detect any specific requests for bananas, fortunes, or jokes.");
        }

        // Step 2: Execute services in parallel
        List<CompletableFuture<?>> futures = new ArrayList<>();

        if (request.bananaArt() != null) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                bananaArtService.generate(request.bananaArt(), context.ai())));
        }
        if (request.fortune() != null) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                fortuneService.generate(request.fortune(), context.ai())));
        }
        if (request.joke() != null) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                jokeService.generate(request.joke(), context.ai())));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Extract results
        BananaArtResult artResult = null;
        FortuneResult fortuneResult = null;
        JokeResult jokeResult = null;

        for (CompletableFuture<?> future : futures) {
            Object res = future.join();
            if (res instanceof BananaArtResult r) artResult = r;
            else if (res instanceof FortuneResult r) fortuneResult = r;
            else if (res instanceof JokeResult r) jokeResult = r;
        }

        // Step 3: Summarize results
        return summarize(new OrchestratedResults(artResult, fortuneResult, jokeResult));
    }

    private OrchestratedResponse summarize(OrchestratedResults results) {
        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (results.bananaArt() != null) {
            if (results.bananaArt().isSuccess()) messages.add(results.bananaArt().art());
            else errors.add("Banana error: " + results.bananaArt().errorMessage());
        }
        if (results.fortune() != null) {
            if (results.fortune().isSuccess()) messages.add(results.fortune().fortune());
            else errors.add("Fortune error: " + results.fortune().errorMessage());
        }
        if (results.joke() != null) {
            if (results.joke().isSuccess()) messages.add(results.joke().joke());
            else errors.add("Joke error: " + results.joke().errorMessage());
        }

        String content = String.join("\n\n", messages);
        if (!errors.isEmpty()) {
            content += (content.isEmpty() ? "" : "\n\n") + "Errors: " + String.join("; ", errors);
        }

        return new OrchestratedResponse(content);
    }

    public record OrchestratedResponse(String message) {}
}
