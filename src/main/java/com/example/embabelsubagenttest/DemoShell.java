package com.example.embabelsubagenttest;

import com.embabel.agent.api.channel.MessageOutputChannelEvent;
import com.embabel.agent.api.channel.OutputChannel;
import com.embabel.agent.api.channel.OutputChannelEvent;
import com.embabel.agent.api.identity.SimpleUser;
import com.embabel.agent.api.identity.User;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.chat.AssistantMessage;
import com.embabel.chat.Chatbot;
import com.embabel.chat.Message;
import com.embabel.chat.UserMessage;
import com.example.embabelsubagenttest.agent.hierarchical.HierarchicalIntentAgent;
import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedIntentAgent;
import com.example.embabelsubagenttest.agent.scattergather.ScatterGatherIntentAgent;
import com.example.embabelsubagenttest.agent.statepattern.StatePatternIntentAgent;
import org.jspecify.annotations.NonNull;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@ShellComponent
record DemoShell(AgentPlatform agentPlatform, Chatbot chatbot) {

    private static final User DEMO_USER = new SimpleUser(
            "demo",
            "Demo User",
            "demo",
            null
    );

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

    @ShellMethod("Chatbot single message (Utility AI) - For multi-turn, use the built-in 'chat' command")
    String intentChatbot(final String content) {
        // Create a queue to receive responses
        BlockingQueue<Message> responseQueue = new ArrayBlockingQueue<>(10);
        OutputChannel outputChannel = new QueueingOutputChannel(responseQueue);

        // Create a new session for this single message
        var session = chatbot.createSession(DEMO_USER, outputChannel, UUID.randomUUID().toString());

        // Send a user message
        session.onUserMessage(new UserMessage(content));

        // Wait for response (with timeout)
        try {
            Message response = responseQueue.poll(60, TimeUnit.SECONDS);
            if (response != null) {
                return response.getContent();
            } else {
                return "Response timed out";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted while waiting for response";
        }
    }

    /**
     * OutputChannel that queues assistant messages for retrieval.
     */
    private record QueueingOutputChannel(BlockingQueue<Message> queue) implements OutputChannel {
        @Override
        public void send(@NonNull OutputChannelEvent event) {
            if (event instanceof MessageOutputChannelEvent msgEvent) {
                Message msg = msgEvent.getMessage();
                if (msg instanceof AssistantMessage) {
                    final boolean _ = queue.offer(msg);
                }
            }
        }
    }
}