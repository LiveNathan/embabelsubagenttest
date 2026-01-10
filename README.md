This project demonstrates advanced agent composition patterns using
the [Embabel framework](https://github.com/embabel/embabel-agent).

Uses Spring Boot 3.5.9 and Embabel 0.3.1.

It highlights two different approaches to building complex, multi-agent systems:

1. **State Machine Pattern** (Current Branch): Explicit state modeling for robust intent handling.
2. **Subagent/Supervisor Pattern** (Main Branch): LLM-driven orchestration.

## Architecture Patterns

### 1. State Machine Pattern (Current Branch)

This branch implements the **State Machine** pattern using Embabel's `@State` API.

* **Entry Point:** `IntentAgent` acts as the state machine host.
* **Logic:**
    1. **Classification:** The initial state classifies the user's intent (Command, Query, or Unknown).
    2. **Transition:** The system transitions to a specific typed state (e.g., `CommandState`, `QueryState`).
    3. **Execution:** Inside the state, further logic (like finer-grained classification) occurs before invoking a
       sub-agent using `AgentInvocation`.
* **Benefits:** Highly deterministic, type-safe, and easy to visualize. The flow is enforced by state transitions.

```mermaid
stateDiagram-v2
    [*] --> Classify: User Input
    state "IntentAgent (State Machine)" as Router {
Classify --> QueryState: Intent = QUERY
Classify --> CommandState: Intent = COMMAND
Classify --> UnknownState: Intent = UNKNOWN

state QueryState {
ProcessQuery --> QueryAgent: Invoke Sub-Agent
}

state CommandState {
ClassifyCommand --> BananaArt: Type = BANANA_ART
ClassifyCommand --> FortuneCookie: Type = FORTUNE
ClassifyCommand --> DadJoke: Type = DAD_JOKE

BananaArt --> BananaArtAgent: Invoke Sub-Agent
FortuneCookie --> FortuneCookieAgent: Invoke Sub-Agent
DadJoke --> DadJokeAgent: Invoke Sub-Agent
}

state UnknownState {
HandleUnknown --> PreTranslationState
}
 }

QueryAgent --> PreTranslationState
BananaArtAgent --> PreTranslationState
FortuneCookieAgent --> PreTranslationState
DadJokeAgent --> PreTranslationState

PreTranslationState --> FinalState: Translate to Portuguese

FinalState --> [*]: Return Response
```

### 2. Hierarchical Subagent Pattern (Main Branch)

The `main` branch demonstrates the **Hierarchical Subagent** pattern.

* **Entry Point:** `IntentAgent`.
* **Logic:**
    1. **IntentAgent** classifies the request and delegates to either `QueryAgent` or `CommandAgent` using
       `RunSubagent`.
    2. **CommandAgent** (if selected) performs a second classification to route to a specialist (Banana, Fortune, Joke).
    3. **Translation:** `IntentAgent` takes the final response from any subagent and translates it into Portuguese
       before returning.
* **Benefits:** Explicit, easy to follow control flow with reusable agents. Allows for post-processing (like
  translation) at higher levels.

```mermaid
graph TD
    User([User]) --> IntentAgent[IntentAgent]

    subgraph "Level 1 Routing"
        IntentAgent -- " Classify " --> IA_LLM[LLM]
        IA_LLM -->|Query| QueryAgent[Query Agent]
        IA_LLM -->|Command| CommandAgent[Command Agent]
    end

    subgraph "Level 2 Routing (CommandAgent)"
        CommandAgent -- " Classify " --> CA_LLM[LLM]
        CA_LLM -->|Banana| ArtAgent[Banana Art Agent]
        CA_LLM -->|Fortune| FortuneAgent[Fortune Cookie Agent]
        CA_LLM -->|Joke| JokeAgent[Dad Joke Agent]
    end

    QueryAgent --> Translate
    ArtAgent --> Translate
    FortuneAgent --> Translate
    JokeAgent --> Translate

    subgraph "Post-Processing"
        Translate[Translate to Portuguese]
    end

    Translate --> IntentAgent
    IntentAgent --> User
```

# Running

Run the shell script to start Embabel under Spring Shell:

```bash
./scripts/shell.sh
```

When the Embabel shell comes up, invoke the intent router:

```bash
intent "Tell me a dad joke about Java"
intent "I want a fortune cookie"
intent "Show me a banana"
intent "Why is the sky blue?"
```

See [DemoShell.java](./src/main/java/com/example/embabelsubagenttest/DemoShell.java) for the implementation.
