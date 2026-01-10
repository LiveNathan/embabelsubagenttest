package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Generates contextual fortune cookie messages with various tones and categories")
public class FortuneCookieAgent {

    @AchievesGoal(description = "Fortune cookie message generated")
    @Action
    public FortuneResponse generateFortune(FortuneRequest request, Ai ai) {
        // Classify the category and tone
        FortuneStyle style = ai.withAutoLlm()
                .withId("classify-fortune-style")
                .creating(FortuneStyle.class)
                .fromPrompt("""
                        Classify the user's fortune cookie preferences:
                        
                        Categories:
                        - CAREER: Work, business, professional success
                        - LOVE: Relationships, romance, connections
                        - WISDOM: General life wisdom (default if not specified)
                        - TECHNOLOGY: Programming, tech, digital life
                        
                        Tones:
                        - MYSTICAL: Mysterious, ancient wisdom
                        - OPTIMISTIC: Positive, uplifting (default if not specified)
                        - PHILOSOPHICAL: Deep, thoughtful
                        - HUMOROUS: Funny, playful
                        
                        User request: %s
                        
                        Return both category and tone.""".formatted(request.description()));

        String fortune = ai.withAutoLlm()
                .withId("generate-fortune-message")
                .generateText("""
                        Generate a fortune cookie message with these characteristics:
                        Category: %s
                        Tone: %s
                        
                        Requirements:
                        - Keep it under 30 words
                        - Make it memorable and impactful
                        - Match the specified category and tone
                        - End with a thought-provoking or uplifting note
                        
                        Generate only the fortune message, no explanation."""
                        .formatted(
                                getCategoryDescription(style.category()),
                                getToneDescription(style.tone())
                        ));

        return new FortuneResponse(fortune);
    }

    private String getCategoryDescription(FortuneCategory category) {
        return switch (category) {
            case CAREER -> "Career and professional success";
            case LOVE -> "Love and relationships";
            case WISDOM -> "General life wisdom";
            case TECHNOLOGY -> "Technology and programming";
        };
    }

    private String getToneDescription(FortuneTone tone) {
        return switch (tone) {
            case MYSTICAL -> "Mysterious and ancient";
            case OPTIMISTIC -> "Positive and uplifting";
            case PHILOSOPHICAL -> "Deep and thoughtful";
            case HUMOROUS -> "Funny and playful";
        };
    }

    public enum FortuneCategory {
        CAREER,
        LOVE,
        WISDOM,
        TECHNOLOGY
    }

    public enum FortuneTone {
        MYSTICAL,
        OPTIMISTIC,
        PHILOSOPHICAL,
        HUMOROUS
    }

    public record FortuneRequest(String description) {
    }

    public record FortuneResponse(String fortune) implements AgentMessageResponse {
        @Override
        public String message() {
            return fortune;
        }
    }

    public record FortuneStyle(FortuneCategory category, FortuneTone tone) {
    }
}
