# Embabelsubagenttest Project Context

## Project Overview
**Embabelsubagenttest** is a Java-based project utilizing the **Embabel framework** (version 0.3.1) and **Spring Boot** (version 3.5.9). It serves as a template and testing ground for developing AI agents capable of interacting with Large Language Models (LLMs).

The project demonstrates:
*   Creating Agents (`@Agent`) with specific goals and personas.
*   Defining Actions (`@Action`) that utilize the `Ai` interface for text generation and structured object creation.
*   Integration with LLM providers (OpenAI, Anthropic, Bedrock).
*   Testing strategies for non-deterministic AI outputs.

## Technology Stack
*   **Language:** Java 25 (Forward-looking version, specified in `pom.xml`)
*   **Frameworks:** Spring Boot 3.5.9, Embabel Agent Framework 0.3.1
*   **Build Tool:** Apache Maven (Wrapper available: `mvnw`)
*   **Testing:** JUnit 5, Spring Boot Test, Embabel Test utilities

## Key Files & Directories

*   `pom.xml`: Project dependencies and configuration. Defines profiles for different model providers (openai, anthropic).
*   `scripts/shell.sh`: The primary entry point to run the interactive agent shell.
*   `src/main/java/com/example/embabelsubagenttest/agent/WriteAndReviewAgent.java`: Core example agent. Demonstrates:
    *   `@Agent` annotation.
    *   `RoleGoalBackstory` and `Persona` definitions.
    *   `@Action` methods (`craftStory`, `reviewStory`).
    *   Using the `Ai` interface to chain LLM calls.
*   `src/test/java/`: Contains Unit (`WriteAndReviewAgentTest.java`) and Integration (`WriteAndReviewAgentIntegrationTest.java`) tests.
*   `docs/llm-docs.md`: Documentation for configuring LLM providers (e.g., Amazon Bedrock).

## Development Workflow

### Building the Project
Use the Maven wrapper to build the application:
```bash
./mvnw clean package
```

### Running the Agent Shell
The project includes a script to launch the interactive Embabel shell:
```bash
./scripts/shell.sh
```
*Note: Ensure `JAVA_HOME` is set to a compatible Java version (25).*

### Testing
Run unit and integration tests:
```bash
./mvnw test
```
Tests utilize `FakeOperationContext` for unit testing (isolating from real LLMs) and `EmbabelMockitoIntegrationTest` for integration testing.

### Configuration
*   **API Keys:** Set environment variables `OPENAI_API_KEY` or `ANTHROPIC_API_KEY` to enable respective profiles.
*   **Properties:** `src/main/resources/application.properties` controls agent settings (e.g., word counts).
*   **Model Selection:** Can be configured via profiles or explicitly in `application.properties` (e.g., `embabel.models.default-llm`).

## Coding Conventions
*   **Agents:** Define agents as Spring beans annotated with `@Agent`.
*   **Actions:** Business logic interacting with LLMs should be in methods annotated with `@Action`.
*   **Dependency Injection:** Use Spring's DI to inject the `Ai` interface or other services.
*   **Immutability:** Prefer Java Records (e.g., `Story`, `ReviewedStory`) for data transfer objects.
