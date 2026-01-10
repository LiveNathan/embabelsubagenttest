package com.example.embabelsubagenttest.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;

@Agent(description = "Tells programming and technology dad jokes with various domains and formats")
public class DadJokeAgent {

    @AchievesGoal(description = "Dad joke generated")
    @Action
    public JokeResponse tellJoke(JokeRequest request, Ai ai) {
        // Classify the domain and format
        JokeStyle style = ai.withAutoLlm()
                .withId("classify-joke-style")
                .creating(JokeStyle.class)
                .fromPrompt("""
                        Classify the user's joke preferences:
                        
                        Domains:
                        - JAVA: Java programming jokes
                        - PYTHON: Python programming jokes
                        - JAVASCRIPT: JavaScript programming jokes
                        - DATABASE: SQL and database jokes
                        - GENERAL: General programming/tech jokes (default if not specified)
                        
                        Formats:
                        - QUESTION_ANSWER: Traditional "Why did X?" setup with punchline
                        - ONE_LINER: Single sentence joke (default if not specified)
                        - PUN: Wordplay and puns
                        
                        User request: %s
                        
                        Return both domain and format.""".formatted(request.description()));

        String joke = ai.withAutoLlm()
                .withId("generate-joke")
                .generateText("""
                        Generate a dad joke with these characteristics:
                        Domain: %s
                        Format: %s
                        
                        Requirements:
                        - Make it wholesome and groan-worthy
                        - Use technical concepts creatively
                        - Keep it appropriate for all audiences
                        - Make it clearly a dad joke (corny, punny, etc.)
                        %s
                        
                        Generate only the joke, no explanation."""
                        .formatted(
                                getDomainDescription(style.domain()),
                                getFormatDescription(style.format()),
                                getFormatInstructions(style.format())
                        ));

        return new JokeResponse(joke);
    }

    private String getDomainDescription(JokeDomain domain) {
        return switch (domain) {
            case JAVA -> "Java programming and JVM";
            case PYTHON -> "Python programming and its features";
            case JAVASCRIPT -> "JavaScript and web development";
            case DATABASE -> "SQL, databases, and data";
            case GENERAL -> "General programming and technology";
        };
    }

    private String getFormatDescription(JokeFormat format) {
        return switch (format) {
            case QUESTION_ANSWER -> "Question and answer format";
            case ONE_LINER -> "One-liner format";
            case PUN -> "Pun/wordplay format";
        };
    }

    private String getFormatInstructions(JokeFormat format) {
        return switch (format) {
            case QUESTION_ANSWER ->
                    "- Start with a question (Why did...? What do you call...? How does...?)\n- Follow with a punchline";
            case ONE_LINER -> "- Write as a single complete sentence or statement";
            case PUN -> "- Focus on wordplay and double meanings\n- Use technical terms creatively";
        };
    }

    public enum JokeDomain {
        JAVA,
        PYTHON,
        JAVASCRIPT,
        DATABASE,
        GENERAL
    }

    public enum JokeFormat {
        QUESTION_ANSWER,
        ONE_LINER,
        PUN
    }

    public record JokeRequest(String description) {
    }

    public record JokeResponse(String joke) implements AgentMessageResponse {
        @Override
        public String message() {
            return joke;
        }
    }

    public record JokeStyle(JokeDomain domain, JokeFormat format) {
    }
}
