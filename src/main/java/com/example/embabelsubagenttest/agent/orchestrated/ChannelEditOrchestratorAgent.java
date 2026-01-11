package com.example.embabelsubagenttest.agent.orchestrated;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedIntentAgent.OrchestratedResponse;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.*;
import com.example.embabelsubagenttest.service.ColorEditService;
import com.example.embabelsubagenttest.service.NameEditService;
import com.example.embabelsubagenttest.service.RouteEditService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Sub-agent responsible for orchestrating complex channel edits.
 * Uses parallel execution for different types of edits.
 */
@Agent(description = "Specialist agent for performing channel name, color, and routing edits")
@Component
public class ChannelEditOrchestratorAgent {

    private final NameEditService nameEditService;
    private final ColorEditService colorEditService;
    private final RouteEditService routeEditService;
    private final StudioConsole studioConsole;

    public ChannelEditOrchestratorAgent(
            NameEditService nameEditService,
            ColorEditService colorEditService,
            RouteEditService routeEditService,
            StudioConsole studioConsole) {
        this.nameEditService = nameEditService;
        this.colorEditService = colorEditService;
        this.routeEditService = routeEditService;
        this.studioConsole = studioConsole;
    }

    @Action
    public OrchestratedResponse handleCommand(OrchestratedIntentAgent.UserIntent.Command command, Ai ai) {
        EditPlan plan = decomposeRequest(command, ai);

        if (plan.isEmpty()) {
            return new OrchestratedResponse("I couldn't identify any specific edits to perform in your request.");
        }

        EditResults results = executeEdits(plan);
        return summarizeResults(results, ai);
    }

    private EditPlan decomposeRequest(OrchestratedIntentAgent.UserIntent.Command command, Ai ai) {
        return ai.withAutoLlm()
                .creating(EditPlan.class)
                .fromPrompt(String.format("""
                        Analyze the following request for a studio mixing console and break it down into specific tasks.
                        The console currently has the following state:
                        %s
                        
                        Request: %s
                        
                        The EditPlan uses a 'SomeOf' pattern. Populate the following lists as applicable:
                        - nameEdits: if the user wants to rename a channel.
                        - colorEdits: if the user wants to change a channel's color (hex).
                        - routeEdits: if the user wants to change where a channel is routed.
                        
                        If a specific edit type is not requested, leave that list null or empty.
                        """, studioConsole.describeChannels(), command.description()));
    }

    private EditResults executeEdits(EditPlan plan) {
        CompletableFuture<List<NameEditResult>> nameFuture = CompletableFuture.supplyAsync(() ->
                (plan.nameEdits() != null && !plan.nameEdits().isEmpty())
                        ? nameEditService.applyEdits(plan.nameEdits(), studioConsole)
                        : Collections.emptyList());

        CompletableFuture<List<ColorEditResult>> colorFuture = CompletableFuture.supplyAsync(() ->
                (plan.colorEdits() != null && !plan.colorEdits().isEmpty())
                        ? colorEditService.applyEdits(plan.colorEdits(), studioConsole)
                        : Collections.emptyList());

        CompletableFuture<List<RouteEditResult>> routeFuture = CompletableFuture.supplyAsync(() ->
                (plan.routeEdits() != null && !plan.routeEdits().isEmpty())
                        ? routeEditService.applyEdits(plan.routeEdits(), studioConsole)
                        : Collections.emptyList());

        return CompletableFuture.allOf(nameFuture, colorFuture, routeFuture)
                .thenApply(v -> new EditResults(
                        nameFuture.join(),
                        colorFuture.join(),
                        routeFuture.join()
                )).join();
    }

    private OrchestratedResponse summarizeResults(EditResults results, Ai ai) {
        String rawResults = String.format("""
                Name Edits: %s
                Color Edits: %s
                Route Edits: %s
                """,
                formatResults(results.nameResults()),
                formatResults(results.colorResults()),
                formatResults(results.routeResults()));

        String summary = ai.withAutoLlm().generateText(String.format("""
                Summarize the following technical operation results into a friendly, professional message for the user.
                Mention successes and clearly list any errors that occurred.
                
                Operation Results:
                %s
                """, rawResults));

        return new OrchestratedResponse(summary);
    }

    private String formatResults(List<?> results) {
        if (results.isEmpty()) return "None";
        return results.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
