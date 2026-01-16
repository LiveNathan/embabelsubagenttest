package com.example.embabelsubagenttest.agent.chatbot;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.chat.AssistantMessage;
import com.embabel.chat.Conversation;
import com.embabel.chat.UserMessage;
import com.example.embabelsubagenttest.agent.chatbot.ChatbotTypes.ChatIntent;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.BananaArtRequest;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.FortuneRequest;
import com.example.embabelsubagenttest.agent.scattergather.CommandTypes.JokeRequest;
import com.example.embabelsubagenttest.service.BananaArtService;
import com.example.embabelsubagenttest.service.FortuneService;
import com.example.embabelsubagenttest.service.JokeService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chatbot actions using Embabel's Utility AI planning.
 * <p>
 * This component demonstrates the chatbot pattern where:
 * - Actions are triggered by UserMessage events
 * - Utility AI selects the best action based on cost/value
 * - Conversation history is maintained in the blackboard
 * - Actions can be rerun for multi-turn conversations
 * <p>
 * Key features:
 * - Uses {@code trigger = UserMessage.class} for reactive message handling
 * - Uses {@code canRerun = true} to allow the action to fire on every message
 * - Uses {@code context.sendMessage()} to send responses back to the conversation
 * - Supports parallel execution of multiple services
 */
@EmbabelComponent
public class ChatbotActions {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that can:
            - Show ASCII art of bananas
            - Provide fortune cookie messages
            - Tell dad jokes (programming/tech themed)
            - Answer general questions
            
            Be friendly and concise in your responses.
            """;

    private final BananaArtService bananaArtService;
    private final FortuneService fortuneService;
    private final JokeService jokeService;

    public ChatbotActions(
            BananaArtService bananaArtService,
            FortuneService fortuneService,
            JokeService jokeService) {
        this.bananaArtService = bananaArtService;
        this.fortuneService = fortuneService;
        this.jokeService = jokeService;
    }

    /**
     * Main chat action that responds to user messages.
     * <p>
     * This action is triggered whenever a UserMessage is added to the blackboard.
     * It classifies the user's intent and routes to appropriate handlers,
     * then sends the response back through the conversation.
     */
    @Action(
            canRerun = true,
            trigger = UserMessage.class,
            description = "Respond to user messages in the chatbot"
    )
    public void respond(Conversation conversation, ActionContext context) {
        String lastMessage = getLastUserMessage(conversation);

        // Classify intent using LLM
        ChatIntent intent = context.ai().withAutoLlm()
                .withId("classify-chat-intent")
                .creating(ChatIntent.class)
                .fromPrompt("""
                        Classify the user's intent into one of:
                        
                        - BANANA_ART: User wants to see ASCII art of a banana
                        - FORTUNE: User wants a fortune cookie message or inspirational quote
                        - JOKE: User wants a dad joke (programming/tech themed)
                        - GENERAL_QUERY: User is asking a general question
                        - MULTIPLE: User wants more than one of banana art, fortune, or joke
                        
                        User message: %s
                        
                        For MULTIPLE: populate only the fields the user is asking for.
                        Example: "show me a banana and tell me a joke" -> bananaArt and joke fields populated
                        """.formatted(lastMessage));

        // Route to appropriate handler based on intent
        String response = switch (intent) {
            case ChatIntent.BananaArt b -> handleBananaArt(b.description(), context);
            case ChatIntent.Fortune f -> handleFortune(f.description(), context);
            case ChatIntent.Joke j -> handleJoke(j.description(), context);
            case ChatIntent.GeneralQuery q -> handleQuery(q.question(), conversation, context);
            case ChatIntent.Multiple m -> handleMultiple(m, context);
        };

        // Send response back to conversation
        var assistantMessage = new AssistantMessage(response);
        context.sendMessage(conversation.addMessage(assistantMessage));
    }

    // --- Intent handlers ---

    private String handleBananaArt(String description, ActionContext context) {
        var result = bananaArtService.generate(
                new BananaArtRequest(description != null ? description : "banana"),
                context.ai());
        return result.isSuccess() ? result.art() : "Error generating banana art: " + result.errorMessage();
    }

    private String handleFortune(String description, ActionContext context) {
        var result = fortuneService.generate(
                new FortuneRequest(description != null ? description : "fortune"),
                context.ai());
        return result.isSuccess() ? result.fortune() : "Error generating fortune: " + result.errorMessage();
    }

    private String handleJoke(String description, ActionContext context) {
        var result = jokeService.generate(
                new JokeRequest(description != null ? description : "joke"),
                context.ai());
        return result.isSuccess() ? result.joke() : "Error generating joke: " + result.errorMessage();
    }

    private String handleQuery(String question, Conversation conversation, ActionContext context) {
        // Use conversation history for context-aware responses
        return context.ai().withAutoLlm()
                .withId("answer-query")
                .withSystemPrompt(SYSTEM_PROMPT)
                .respond(conversation.getMessages())
                .getContent();
    }

    private String handleMultiple(ChatIntent.Multiple intent, ActionContext context) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        if (intent.bananaArt() != null) {
            futures.add(CompletableFuture.supplyAsync(() ->
                    handleBananaArt(intent.bananaArt(), context)));
        }
        if (intent.fortune() != null) {
            futures.add(CompletableFuture.supplyAsync(() ->
                    handleFortune(intent.fortune(), context)));
        }
        if (intent.joke() != null) {
            futures.add(CompletableFuture.supplyAsync(() ->
                    handleJoke(intent.joke(), context)));
        }

        if (futures.isEmpty()) {
            return "I didn't understand what you wanted. Try asking for a banana, fortune, or joke!";
        }

        // Wait for all to complete and join results
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return String.join("\n\n---\n\n", results);
    }

    // --- Helper methods ---

    private String getLastUserMessage(Conversation conversation) {
        var messages = conversation.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            var msg = messages.get(i);
            if (msg instanceof UserMessage userMessage) {
                return userMessage.getContent();
            }
        }
        return "";
    }
}
