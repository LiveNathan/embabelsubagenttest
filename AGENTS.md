# Embabel Agent Design Patterns

This project demonstrates three advanced agent composition patterns using the **Embabel framework**. These designs are implemented in separate packages to allow side-by-side comparison.

## Architecture Patterns

### 1. Scatter-Gather (Parallel GOAP)
**Package:** `com.example.embabelsubagenttest.agent.scattergather`

This pattern optimizes for performance by executing independent sub-tasks in parallel.

*   **Entry Point:** `ScatterGatherIntentAgent`
*   **Shell Command:** `intent-scatter-gather "Show me a banana and tell me a joke"`
*   **Key Components:**
    *   `CommandOrchestrator`: Uses `ScatterGatherBuilder` to execute multiple specialist services concurrently.
    *   `ScatterGatherQueryAgent`: Handles general queries.
    *   **Services:** `BananaArtService`, `FortuneService`, `JokeService` (Plain Spring `@Component`s, not agents).
*   **Best For:** Scenarios where a single request can be broken down into multiple independent actions that don't depend on each other's output.

### 2. Hierarchical (Supervisor/Subagent)
**Package:** `com.example.embabelsubagenttest.agent.hierarchical`

A classic recursive delegation pattern where a supervisor agent routes tasks to specialized sub-agents.

*   **Entry Point:** `HierarchicalIntentAgent`
*   **Shell Command:** `intent-hierarchical "Show me a banana"`
*   **Key Components:**
    *   `HierarchicalIntentAgent`: Top-level router.
    *   `HierarchicalCommandAgent`: Secondary router for commands.
    *   **Sub-Agents:** `HierarchicalBananaArtAgent`, `HierarchicalFortuneCookieAgent`, `HierarchicalDadJokeAgent`.
*   **Best For:** Complex domains with deep taxonomy where tasks require specialized handling logic encapsulated in distinct agents.

### 3. State Pattern (State Machine)
**Package:** `com.example.embabelsubagenttest.agent.statepattern`

Uses Embabel's `@State` annotation to model the conversation as a state machine.

*   **Entry Point:** `StatePatternIntentAgent`
*   **Shell Command:** `intent-state-pattern "Tell me a joke"`
*   **Key Components:**
    *   `IntentState` (Sealed Interface): Defines the states (Query, Command, Multiple, Unknown).
    *   `CommandState`, `QueryState`: Records implementing `IntentState` with `@Action` methods.
    *   `MultiIntentState`: Handles complex flows within the state machine.
*   **Best For:** Complex multi-turn conversations or workflows where the valid actions depend strictly on the current context/state of the interaction.

## Running the Examples

1.  **Build the project:**
    ```bash
    ./mvnw clean package
    ```

2.  **Start the Agent Shell:**
    ```bash
    ./scripts/shell.sh
    ```

3.  **Invoke the Agents:**
    Inside the shell, use the specific commands for each pattern:

    ```bash
    # Parallel execution
    intent-scatter-gather "Show me a banana and tell me a joke"

    # Hierarchical routing
    intent-hierarchical "Show me a banana"

    # State machine flow
    intent-state-pattern "Tell me a joke"
    ```

## Coding Conventions
*   **Agents:** Define agents as Spring beans annotated with `@Agent`.
*   **Actions:** Business logic interacting with LLMs should be in methods annotated with `@Action`.
*   **Dependency Injection:** Use Spring's DI to inject the `Ai` interface or other services.
*   **Immutability:** Prefer Java Records (e.g., `Story`, `ReviewedStory`) for data transfer objects.