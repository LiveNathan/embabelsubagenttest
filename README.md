# Embabel Agent Design Patterns

This project demonstrates three advanced agent composition patterns using the **Embabel framework**. These designs are
implemented in separate packages to allow side-by-side comparison.

## Architecture Patterns

### 1. Scatter-Gather (Parallel GOAP)

**Package:** `com.example.embabelsubagenttest.agent.scattergather`

This pattern optimizes for performance by executing independent sub-tasks in parallel.

* **Entry Point:** `ScatterGatherIntentAgent`
* **Shell Command:** `intent-scatter-gather "Show me a banana and tell me a joke"`
* **Key Components:**
    * `CommandOrchestrator`: Uses `ScatterGatherBuilder` to execute multiple specialist services concurrently.
    * `ScatterGatherQueryAgent`: Handles general queries.
    * **Services:** `BananaArtService`, `FortuneService`, `JokeService` (Plain Spring `@Component`s, not agents).
* **Best For:** Scenarios where a single request can be broken down into multiple independent actions that don't depend
  on each other's output.

### 2. Hierarchical (Supervisor/Subagent)

**Package:** `com.example.embabelsubagenttest.agent.hierarchical`

A classic recursive delegation pattern where a supervisor agent routes tasks to specialized sub-agents.

* **Entry Point:** `HierarchicalIntentAgent`
* **Shell Command:** `intent-hierarchical "Show me a banana"`
* **Key Components:**
    * `HierarchicalIntentAgent`: Top-level router.
    * `HierarchicalCommandAgent`: Secondary router for commands.
    * **Sub-Agents:** `HierarchicalBananaArtAgent`, `HierarchicalFortuneCookieAgent`, `HierarchicalDadJokeAgent`.
* **Best For:** Complex domains with deep taxonomy where tasks require specialized handling logic encapsulated in
  distinct agents.

### 3. State Pattern (State Machine)

**Package:** `com.example.embabelsubagenttest.agent.statepattern`

Uses Embabel's `@State` annotation to model the conversation as a state machine.

* **Entry Point:** `StatePatternIntentAgent`
* **Shell Command:** `intent-state-pattern "Tell me a joke"`
* **Key Components:**
    * `IntentState` (Sealed Interface): Defines the states (Query, Command, Multiple, Unknown).
    * `CommandState`, `QueryState`: Records implementing `IntentState` with `@Action` methods.
    * `MultiIntentState`: Handles complex flows within the state machine.
* **Best For:** Complex multi-turn conversations or workflows where the valid actions depend strictly on the current
  context/state of the interaction.

### 4. Orchestrated (Parallel Service Orchestration)

**Package:** `com.example.embabelsubagenttest.agent.orchestrated`

Refactored pattern that routes requests to an orchestrator which dynamically selects and executes services in parallel
using `CompletableFuture`.

* **Entry Point:** `OrchestratedIntentAgent`
* **Shell Command:** `intent-orchestrated "Show me a banana and tell me a joke"`
* **Key Components:**
    * `OrchestratedIntentAgent`: Routes to Command or Query agents.
    * `OrchestratedCommandAgent`: Uses LLM to populate `OrchestratedRequest` (implementing `SomeOf`) to determine needed
      services, then runs them in parallel.
    * `OrchestratedQueryAgent`: Handles general queries.
* **Best For:** Scenarios requiring dynamic selection and parallel execution of independent services based on natural
  language intent.

### 5. Chatbot (Utility AI Multi-Turn Conversation)

**Package:** `com.example.embabelsubagenttest.agent.chatbot`

Uses Embabel's native chatbot architecture with Utility AI planning, optimized for multi-turn conversations
as described in the [Embabel Chatbot documentation](docs/build-chatbot.md).

* **Entry Point:** `ChatbotActions` (with `@EmbabelComponent`)
* **Shell Commands:**
    * `intent-chatbot "Show me a banana"` - Single message
    * `chat` - Built-in multi-turn conversation mode
* **Key Components:**
    * `ChatbotActions`: `@EmbabelComponent` with actions triggered by `UserMessage`
    * `ChatbotConfiguration`: Creates `AgentProcessChatbot.utilityFromPlatform()` for Utility AI planning
    * `ChatbotTypes`: Domain types for intent classification
* **Key Features:**
    * `trigger = UserMessage.class` - Actions fire on user messages
    * `canRerun = true` - Allows repeated execution for multi-turn chat
    * `context.sendMessage()` - Sends responses back to conversation
    * Conversation history maintained in blackboard
    * Utility AI selects best action based on cost/value scoring
* **Best For:** Multi-turn conversational interfaces, chatbots, and scenarios where action selection
  should be based on scoring rather than explicit routing.

## Running the Examples

1. **Build the project:**
   ```bash
   ./mvnw clean package
   ```

2. **Start the Agent Shell:**
   ```bash
   ./scripts/shell.sh
   ```

3. **Invoke the Agents:**
   Inside the shell, use the specific commands for each pattern:

   ```bash
   # Parallel execution
   intent-scatter-gather "Show me a banana and tell me a joke"

   # Hierarchical routing
   intent-hierarchical "Show me a banana"

   # State machine flow
   intent-state-pattern "Tell me a joke"

   # Orchestrated flow
   intent-orchestrated "Show me a banana and tell me a joke"

   # Chatbot (single message)
   intent-chatbot "Show me a banana and tell me a joke"

   # Chatbot (multi-turn conversation)
   chat
   ```

## Coding Conventions

* **Agents:** Define agents as Spring beans annotated with `@Agent`.
* **Actions:** Business logic interacting with LLMs should be in methods annotated with `@Action`.
* **Dependency Injection:** Use Spring's DI to inject the `Ai` interface or other services.
* **Immutability:** Prefer Java Records (e.g., `Story`, `ReviewedStory`) for data transfer objects.