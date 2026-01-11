package com.example.embabelsubagenttest.service;

import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.NameEditResult;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.NameEditTask;
import com.example.embabelsubagenttest.agent.orchestrated.StudioConsole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for applying name edits to the StudioConsole.
 */
@Service
public class NameEditService {

    /**
     * Applies a list of name edit tasks to the provided console.
     * Each task is executed in isolation; failures are caught and returned as error results.
     */
    public List<NameEditResult> applyEdits(List<NameEditTask> tasks, StudioConsole console) {
        List<NameEditResult> results = new ArrayList<>();
        if (tasks == null) {
            return results;
        }

        for (NameEditTask task : tasks) {
            try {
                console.setChannelName(task.channelNumber(), task.newName());
                results.add(NameEditResult.success(
                        task.channelNumber(), 
                        String.format("Channel %d renamed to '%s'", task.channelNumber(), task.newName())
                ));
            } catch (Exception e) {
                results.add(NameEditResult.error(
                        task.channelNumber(), 
                        String.format("Failed to rename channel %d: %s", task.channelNumber(), e.getMessage())
                ));
            }
        }

        return results;
    }
}
