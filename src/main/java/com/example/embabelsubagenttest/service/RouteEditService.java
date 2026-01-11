package com.example.embabelsubagenttest.service;

import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.RouteEditResult;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.RouteEditTask;
import com.example.embabelsubagenttest.agent.orchestrated.StudioConsole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for applying routing edits to the StudioConsole.
 */
@Service
public class RouteEditService {

    /**
     * Applies a list of route edit tasks to the provided console.
     * Each task is executed in isolation; failures are caught and returned as error results.
     */
    public List<RouteEditResult> applyEdits(List<RouteEditTask> tasks, StudioConsole console) {
        List<RouteEditResult> results = new ArrayList<>();
        if (tasks == null) {
            return results;
        }

        for (RouteEditTask task : tasks) {
            try {
                console.setChannelRoute(task.channelNumber(), task.destination());
                results.add(RouteEditResult.success(
                        task.channelNumber(),
                        String.format("Channel %d routed to '%s'", task.channelNumber(), task.destination())
                ));
            } catch (Exception e) {
                results.add(RouteEditResult.error(
                        task.channelNumber(),
                        String.format("Failed to route channel %d: %s", task.channelNumber(), e.getMessage())
                ));
            }
        }

        return results;
    }
}
