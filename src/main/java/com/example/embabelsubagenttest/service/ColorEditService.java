package com.example.embabelsubagenttest.service;

import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.ColorEditResult;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.ColorEditTask;
import com.example.embabelsubagenttest.agent.orchestrated.StudioConsole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for applying color edits to the StudioConsole.
 */
@Service
public class ColorEditService {

    /**
     * Applies a list of color edit tasks to the provided console.
     * Each task is executed in isolation; failures are caught and returned as error results.
     */
    public List<ColorEditResult> applyEdits(List<ColorEditTask> tasks, StudioConsole console) {
        List<ColorEditResult> results = new ArrayList<>();
        if (tasks == null) {
            return results;
        }

        for (ColorEditTask task : tasks) {
            try {
                console.setChannelColor(task.channelNumber(), task.hexColor());
                results.add(ColorEditResult.success(
                        task.channelNumber(),
                        String.format("Channel %d color set to '%s'", task.channelNumber(), task.hexColor())
                ));
            } catch (Exception e) {
                results.add(ColorEditResult.error(
                        task.channelNumber(),
                        String.format("Failed to set color for channel %d: %s", task.channelNumber(), e.getMessage())
                ));
            }
        }

        return results;
    }
}
