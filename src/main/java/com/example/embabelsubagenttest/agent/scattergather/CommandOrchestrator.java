package com.example.embabelsubagenttest.agent.scattergather;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.workflow.control.ScatterGatherBuilder;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.*;
import com.example.embabelsubagenttest.service.BananaArtService;
import com.example.embabelsubagenttest.service.FortuneService;
import com.example.embabelsubagenttest.service.JokeService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Orchestrates command execution by delegating to specialized services.
 * Uses ScatterGatherBuilder for parallel execution when multiple commands are requested.
 * Single-action pattern to avoid GOAP planning complexity.
 */
@Agent(description = "Orchestrates command execution by delegating to specialized services")
public class CommandOrchestrator {

    private final BananaArtService bananaArtService;
    private final FortuneService fortuneService;
    private final JokeService jokeService;

    public CommandOrchestrator(
            BananaArtService bananaArtService,
            FortuneService fortuneService,
            JokeService jokeService) {
        this.bananaArtService = bananaArtService;
        this.fortuneService = fortuneService;
        this.jokeService = jokeService;
    }

    /**
     * Single action that handles the entire command flow:
     * 1. Classifies the command using LLM (SomeOf pattern)
     * 2. Executes applicable services in parallel (ScatterGatherBuilder)
     * 3. Consolidates results into user-facing message
     */
    @AchievesGoal(description = "Command executed successfully")
    @Action
    public CommandOrchestratorResponse handleCommand(
            ScatterGatherIntentAgent.UserIntent.Command command,
            ActionContext context) {

        // Step 1: Classify command to determine which services to invoke
        CommandRequest request = classifyCommand(command, context.ai());

        // Step 2: Execute services in parallel
        CommandResults results = executeCommands(request, context);

        // Step 3: Consolidate and return
        return summarizeResults(results);
    }

    /**
     * Classifies the command using LLM with SomeOf pattern.
     * LLM populates only applicable fields.
     */
    private CommandRequest classifyCommand(ScatterGatherIntentAgent.UserIntent.Command command, Ai ai) {
        return ai.withAutoLlm()
                .withId("classify-command")
                .creating(CommandRequest.class)
                .fromPrompt("""
                        Analyze the user's command and determine which services should be invoked.
                        You can populate one or more of the following fields:

                        - bananaArt: If the user wants ASCII art of a banana
                        - fortune: If the user wants a fortune cookie message or inspirational quote
                        - joke: If the user wants a dad joke

                        User command: %s
                        
                        Examples:
                        - "Show me a banana" → populate only bananaArt with description
                        - "Tell me a joke" → populate only joke with description
                        - "Show me a banana and tell me a joke" → populate both bananaArt and joke
                        - "Give me a fortune, a banana, and a joke" → populate all three
                        
                        For each applicable service, provide a description extracted from the user's request.
                        Leave other fields null.""".formatted(command.description()));
    }

    /**
     * Executes commands in parallel using ScatterGatherBuilder.
     * Only invokes services where the request field is non-null.
     */
    private CommandResults executeCommands(CommandRequest request, ActionContext context) {
        if (request.isEmpty()) {
            return new CommandResults(null, null, null);
        }

        List<Supplier<Object>> tasks = new ArrayList<>();

        // Add tasks only for non-null requests
        if (request.bananaArt() != null) {
            final BananaArtRequest artRequest = request.bananaArt();
            tasks.add(() -> bananaArtService.generate(artRequest, context.ai()));
        }
        if (request.fortune() != null) {
            final FortuneRequest fortuneRequest = request.fortune();
            tasks.add(() -> fortuneService.generate(fortuneRequest, context.ai()));
        }
        if (request.joke() != null) {
            final JokeRequest jokeRequest = request.joke();
            tasks.add(() -> jokeService.generate(jokeRequest, context.ai()));
        }

        // If only one task, execute directly (no need for parallel execution)
        if (tasks.size() == 1) {
            Object result = tasks.get(0).get();
            return createResultsFromSingle(result);
        }

        // Use ScatterGatherBuilder for parallel execution of multiple tasks
        return ScatterGatherBuilder
                .returning(CommandResults.class)
                .fromElements(Object.class)
                .generatedBy(tasks)
                .consolidatedBy(ctx -> {
                    BananaArtResult artResult = null;
                    FortuneResult fortuneResult = null;
                    JokeResult jokeResult = null;

                    // Map results back based on types
                    for (Object result : ctx.getInput().getResults()) {
                        if (result instanceof BananaArtResult r) {
                            artResult = r;
                        } else if (result instanceof FortuneResult r) {
                            fortuneResult = r;
                        } else if (result instanceof JokeResult r) {
                            jokeResult = r;
                        }
                    }

                    return new CommandResults(artResult, fortuneResult, jokeResult);
                })
                .asSubProcess(context);
    }

    private CommandResults createResultsFromSingle(Object result) {
        if (result instanceof BananaArtResult r) {
            return new CommandResults(r, null, null);
        } else if (result instanceof FortuneResult r) {
            return new CommandResults(null, r, null);
        } else if (result instanceof JokeResult r) {
            return new CommandResults(null, null, r);
        }
        return new CommandResults(null, null, null);
    }

    /**
     * Consolidates results into a user-facing message.
     */
    private CommandOrchestratorResponse summarizeResults(CommandResults results) {
        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (results.bananaArt() != null) {
            if (results.bananaArt().isSuccess()) {
                messages.add(results.bananaArt().art());
            } else {
                errors.add("Banana art failed: " + results.bananaArt().errorMessage());
            }
        }

        if (results.fortune() != null) {
            if (results.fortune().isSuccess()) {
                messages.add(results.fortune().fortune());
            } else {
                errors.add("Fortune failed: " + results.fortune().errorMessage());
            }
        }

        if (results.joke() != null) {
            if (results.joke().isSuccess()) {
                messages.add(results.joke().joke());
            } else {
                errors.add("Joke failed: " + results.joke().errorMessage());
            }
        }

        StringBuilder response = new StringBuilder();
        if (!messages.isEmpty()) {
            response.append(String.join("\n\n", messages));
        }
        if (!errors.isEmpty()) {
            if (!messages.isEmpty()) {
                response.append("\n\n");
            }
            response.append("Errors: ").append(String.join("; ", errors));
        }
        if (messages.isEmpty() && errors.isEmpty()) {
            response.append("I couldn't understand that command. Try asking for a banana, fortune, or joke!");
        }

        return new CommandOrchestratorResponse(response.toString());
    }

    public record CommandOrchestratorResponse(String message) implements AgentMessageResponse {
    }
}
