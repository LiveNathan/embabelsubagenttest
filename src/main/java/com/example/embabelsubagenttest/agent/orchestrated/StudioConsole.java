package com.example.embabelsubagenttest.agent.orchestrated;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A mock domain object representing a mixing console.
 * This class maintains the state of console channels and provides methods for manipulation and description.
 */
public class StudioConsole {

    public record ChannelState(String name, String color, String routeDestination) {
    }

    private final Map<Integer, ChannelState> channels = new HashMap<>();

    public StudioConsole() {
        // Initialize with some default channels
        channels.put(1, new ChannelState("Kick", "#FF0000", "Master"));
        channels.put(2, new ChannelState("Snare", "#00FF00", "Master"));
        channels.put(3, new ChannelState("Vocals", "#0000FF", "VocalBus"));
    }

    public ChannelState getChannel(int channelNumber) {
        return channels.getOrDefault(channelNumber, new ChannelState("Empty", "#CCCCCC", "None"));
    }

    public void setChannelName(int channelNumber, String name) {
        ChannelState current = getChannel(channelNumber);
        channels.put(channelNumber, new ChannelState(name, current.color(), current.routeDestination()));
    }

    public void setChannelColor(int channelNumber, String color) {
        ChannelState current = getChannel(channelNumber);
        channels.put(channelNumber, new ChannelState(current.name(), color, current.routeDestination()));
    }

    public void setChannelRoute(int channelNumber, String destination) {
        ChannelState current = getChannel(channelNumber);
        channels.put(channelNumber, new ChannelState(current.name(), current.color(), destination));
    }

    /**
     * Provides a string description of all channels for LLM context.
     */
    public String describeChannels() {
        if (channels.isEmpty()) {
            return "The console is currently empty.";
        }
        return channels.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> String.format("Channel %d: Name=%s, Color=%s, Route=%s",
                        e.getKey(), e.getValue().name(), e.getValue().color(), e.getValue().routeDestination()))
                .collect(Collectors.joining("\n", "Current Console State:\n", ""));
    }

    public Map<Integer, ChannelState> getChannels() {
        return Collections.unmodifiableMap(channels);
    }
}
