package com.example.embabelsubagenttest.agent.chatbot;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Verbosity;
import com.embabel.chat.Chatbot;
import com.embabel.chat.agent.AgentProcessChatbot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the chatbot pattern using Utility AI planning.
 * <p>
 * This configuration creates a utility-based chatbot that:
 * - Automatically discovers all {@code @Action} methods from {@code @EmbabelComponent} classes
 * - Uses Utility AI to score and select the best action for each situation
 * - Supports multi-turn conversations via the Conversation blackboard object
 * <p>
 * The chatbot can be accessed via the shell using the 'chat' command.
 */
@Configuration
public class ChatbotConfiguration {

    /**
     * Creates a utility-based chatbot using AgentProcessChatbot.
     * <p>
     * Utility AI planning evaluates all available actions and selects the one
     * with the highest value (lowest cost) whose preconditions are satisfied.
     * This allows for flexible routing without explicit if-else chains.
     *
     * @param agentPlatform The agent platform containing all registered agents and actions
     * @return A configured Chatbot instance
     */
    @Bean
    Chatbot chatbot(AgentPlatform agentPlatform) {
        return AgentProcessChatbot.utilityFromPlatform(
                agentPlatform,
                new Verbosity().showPrompts()
        );
    }
}
