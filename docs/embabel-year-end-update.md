Embabel Year-End Update: Building The Best Agent Framework
==========================================================

[![Rod Johnson](https://miro.medium.com/v2/resize:fill:64:64/0*w7FhiB6igokPDlHh.jpeg)](https://medium.com/@springrod?source=post_page---byline--25ed98728e79---------------------------------------)

[Rod Johnson](https://medium.com/@springrod?source=post_page---byline--25ed98728e79---------------------------------------)

8 min read

·

4 days ago

[nameless link](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2F_%2Fvote%2Fp%2F25ed98728e79&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40springrod%2Fembabel-year-end-update-building-the-best-agent-framework-25ed98728e79&user=Rod+Johnson&userId=de5947672915&source=---header_actions--25ed98728e79---------------------clap_footer------------------)

--

3

[nameless link](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2F_%2Fbookmark%2Fp%2F25ed98728e79&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40springrod%2Fembabel-year-end-update-building-the-best-agent-framework-25ed98728e79&source=---header_actions--25ed98728e79---------------------bookmark_footer------------------)

Listen

Share

I’ve been quiet recently, because we’ve been focused on engineering — perfecting
the [Embabel](https://github.com/embabel/embabel-agent) API and extending the framework’s capabilities.

From day one, Embabel has offered what no other framework does:
sophisticated, [deterministic GOAP planning](https://medium.com/@springrod/ai-for-your-gen-ai-how-and-why-embabel-plans-3930244218f6);
type-safe domain modelling at the core; and the ability to chain actions _safely_ in ways you never explicitly coded.
Given our goal of being _the_ agent framework for enterprise, we’re focused on minimizing nondeterminism and integrating
with existing business systems.

On this solid foundation, we’re building richer and richer functionality, with a consistent, integrated API. We’re
emphasizing ease of use and removing friction points in using Embabel, informed by building real applications.

**We’re now ensuring that anything valuable you can do with any other framework, you can do better with Embabel.** Not
just the sophisticated deterministic flows we pioneered — _everything_. Utility-based exploration, the supervisor
pattern, state machines, chatbots, RAG. All of it, with the type safety and testability that the JVM, and our unique
design, provides.

Here are some recent advances. More depth on each coming soon.

Planner Enhancements
--------------------

We’ve added two new planner types: **Utility** and **Supervisor**, and introduced a state machine model that works with
either. Both use our existing programming model, with strongly typed `@Action` methods.

### Utility AI Planner: Great for Exploration

GOAP is great when you know what goal you want to achieve, and want the framework to figure out a path to achieve it.

[Utility AI](https://en.wikipedia.org/wiki/Utility_system) — another algorithm
from [gaming](https://www.aiandgames.com/p/ai-101-introducing-utility-ai) — is great for _exploration_ — when you don’t
have a predetermined goal. It makes decisions based on immediate value — perfect for event-driven systems, chatbots, or
reactive processing where the “right” action depends on what you discover as you progress. The planner chooses the most
valuable action it can run at a given time, executes it, then replans. It may discover an unexpected path, although it
will never execute an action whose conditions were not satisfied. Or it may find that exploration has concluded and
there’s nothing more to do, and this is not an error.

Utility AI works especially well with our recently introduced dynamic cost and value. This enables the value of actions
to change in real time — for example, if you want different behaviour outside core business hours, or want to respond
differently depending on the load on an external system.

```
@Cost(name = "triageCost")
public double computeProcessingCost(GithubIssue issue) {
   // return calculation based on objects from Blackboard
}
@Action(costMethod="triageCost")
IssueResponse triageIssue(GitHubIssue issue, Ai ai) {
    return ai.createObject("Triage this issue", IssueResponse.class);
}
```

### Supervisor Planner: Like LangGraph or Google ADK, But Typed

We now support the popular **supervisor** pattern — an LLM deciding which actions to call, viewing them as tools.

**Supervisor planning is convenient, but not deterministic, hence is not suitable for many business critical tasks**.
But our implementation is superior to that of other frameworks in that it is _type-informed_: the LLM sees schemas,
outputs are validated, and you get semi-deterministic behaviour, as action tools are only exposed if their preconditions
are satisfied. Also interim structure is written to the blackboard, so you can persist it or respond to events with
structure.

Consider this Google ADK code:

```
# Define sub-agents
greeter = LlmAgent(
    name="greeter",
    model="gemini-2.5-flash",
    instruction="You handle greetings warmly.",
    output_key="greeting_result"  # String key
)
task_executor = LlmAgent(
    name="task_executor",
    model="gemini-2.5-flash",
    instruction="Process data from {greeting_result}.",  # Magic string reference
    output_key="task_result"
)
# Create coordinator
coordinator = LlmAgent(
    name="Coordinator",
    model="gemini-2.5-flash",
    description="I coordinate greetings and tasks.",
    sub_agents=[greeter, task_executor]
)
```

In Embabel it would look as follows:

```
@EmbabelComponent
public class CoordinatorActions {
   @Action
    GreetingResult greet(UserInput input, Ai ai) {
        return ai.createObject("Handle this greeting warmly", GreetingResult.class);
    }
    @AchievesGoal("user has been greeted")
    @Action
    TaskResult executeTask(GreetingResult greeting, Ai ai) {
        return ai.createObject("Process: " + greeting.message(), TaskResult.class);
    }
}
// Run with Supervisor planner
var result = SupervisorInvocation.on(agentPlatform)
    .returning(TaskResult.class)
    .withScope(AgentScopeBuilder.fromInstance(coordinatorActions))
    .invoke(new UserInput(request));
```

No magic output keys; typed and testable.

> The same actions work across all planners. Write once, deploy with GOAP for determinism, Utility for exploration, or
> Supervisor for flexibility.

### @State: State Machines Done Right

LangGraph popularized state machines for agents. We’ve made them type-safe, refactorable, and maintainable.

Our`@State` annotation enables branching, looping, and human-in-the-loop without magic strings. Any `@Action` method can
return a class annotated with `@State` to trigger a transition. Only the actions on the target state will be available
until the agent process terminates, or another state transition occurs. Polymorphism support helps routing between
states, allowing us to declare a supertype as action return:

```
@State
record AssessStory(String content) implements Stage {
    @Action
    Stage assess() {
        if (isAcceptable()) return new Done(content);
        return new ReviseStory(content);  // Loop back
    }
}
@State
record ReviseStory(String content) implements Stage {
    @Action
    AssessStory revise() {
        return new AssessStory(improvedContent());
    }
}
@State
record Done(String content) implements Stage {
    @AchievesGoal(description = "Story finalized")
    @Action
    Output complete() {
        return new Output(content);
    }
}
```

States take the data they need in their constructors. Transitions are type-checked. IDEs understand them.

Let’s see how this would look in LangGraph:

```
# Define the state schema
class State(TypedDict):
    content: str
    status: str
# Define node functions
def assess_story(state: State) -> State:
    # ... assessment logic
    return {"content": state["content"], "status": "acceptable"}  # or "needs_revision"def revise_story(state: State) -> State:
    # ... revision logic
    return {"content": revised_content, "status": "pending"}
def finalize(state: State) -> State:
    return state
# Routing function
def should_revise(state: State) -> str:
    if state["status"] == "acceptable":
        return "done"
    return "revise"
# Build the graph
workflow = StateGraph(State)
workflow.add_node("assess", assess_story)
workflow.add_node("revise", revise_story)
workflow.add_node("done", finalize)
workflow.set_entry_point("assess")
workflow.add_conditional_edges("assess", should_revise, {"revise": "revise", "done": "done"})
workflow.add_edge("revise", "assess")
workflow.add_edge("done", END)
graph = workflow.compile()
```

To say this doesn’t compare well is an understatement. LangGraph = magic strings everywhere. Routing logic is separated
from the states it routes between. No compile-time checking that “revise” actually exists. Good luck testing it; rename
a node and watch your graph break.

> Embabel now implements the core idea of LangGraph in a far superior way.

### Putting it all together

Not only is the programming model identical between all planner types; you can combine them. If an action is run with
the GOAP planner and returns an `@State` type, control goes to the state machine, but its ultimate return will be passed
back to the GOAP planner; each state defaults to GOAP planning internally. You can use any set of actions with any
planner, depending on your requirements.

Looping in particular becomes much easier, as it’s the one common thing that is hard to express purely in GOAP.

Tool Enhancements
-----------------

We’ve significantly enhanced our tool support, along with shipping powerful tools for developers to use.

### AgenticTool: Familiar Pattern, Better Model

If you’re coming from LangChain or Google ADK, `AgenticTool` provides a familiar pattern: an LLM orchestrating
sub-tools. It maps directly to LangChain's supervisor-with-workers or ADK's coordinator pattern.

```
var researcher = new AgenticTool(
        "research-assistant",
        "Research topics using web search and summarization")
    .withTools(searchTool, summarizeTool)
    .withLlm(LlmOptions.withModel("gpt-4"));
context.ai()
    .withTool(researcher)
    .generateText("What's the latest on quantum computing?");
```

It operates at the _tool_, not _agent,_ level. It’s **composable** and reusable — just a tool, usable anywhere tools can
be used. And it’s simple— no graph-based workflows or explicit Sequential/Parallel/Loop patterns.

> In largely detyped frameworks like LangGraph and ADK, the distinction between a “supervisor agent” and an “agentic
> tool” is often meaningless. It’s all strings passing around. The hierarchy is conceptual, not enforced. In Embabel, this
> distinction matters because we have typed composition at the agent level (GOAP, `_@State_`) versus simpler tool-level
> orchestration (`_AgenticTool_`). You choose the right abstraction for the job.

For anything beyond simple orchestration, use GOAP, Utility, or `@State`—deterministic, type-safe planning that's more
powerful than LLM-driven orchestration.

### Agentic RAG API

Although it’s one of the oldest applications of Gen AI, RAG is notoriously hard to get right. We believe that the best
approach is **agentic RAG**, and offer a simple, elegant API for it.

The new `ToolishRag` class exposes search operations as a set of LLM tools, along with instructions on their use. The
LLM decides when to search, what queries to run, and how to expand context.

You give it a name, a description and a `SearchOperations` implementation. Depending on the capabilities of that
implementation, `ToolishRag` will expose tools for vector search, text search, chunk expansion, and any operations
unique to that store.

```
var response = context.ai()
    .withReference(
      new ToolishRag("docs", "Embabel Documentation", searchOperations)
    )
    .respondWithSystemPrompt(conversation, bindings);
```

Agentic RAG without boilerplate, consistent across stores. You can even use multiple `ToolishRag` instances in one LLM
interaction and the framework will disambiguate their tools.

We ship `SearchOperations` implementations for Neo4j, Lucene, local directories and over any Spring AI `VectorStore`. We
will add a full-featured PostgreSQL implementation using pgvector and full text search. It’s also easy to implement your
own `SearchOperations` and benefit from the same elegant programming model.

Chatbots and Developer Resources
--------------------------------

### Easy Chatbot Authoring

As of `0.3.1` it is much easier
to [write chatbots in Embabel](https://docs.embabel.com/embabel-agent/guide/0.3.1-SNAPSHOT/#building-a-chatbot).

The Utility planner is ideal for chatbots, and the new `trigger` field on `@Action` enables reactive chatbot patterns,
causing the action to fire only when a specific type is most recent on the blackboard:

```
@Action(canRerun = true, trigger = UserMessage.class)
void respond(Conversation conversation, ActionContext context) {
    var reply = context.ai()
        .withTemplate("chatbot")
        .respondWithSystemPrompt(conversation, Map.of());
    context.sendMessage(conversation.addMessage(reply));
}
```

Multiple actions can respond to messages with different values. The planner picks the most valuable. The `@State`
mechanism also works well with chatbots.

See the [https://github.com/embabel/ragbot](https://github.com/embabel/ragbot) repository for an example of a chatbot
using RAG.

![RAG chat with ragbot](https://miro.medium.com/v2/resize:fit:1400/format:webp/1*tOuOd2luYMLywHpn44wwUQ.jpeg)

### The Guide: MCP Server for Embabel Development

We’ve used our RAG support to build an MCP server in the new `guide` project that helps you build with Embabel. It uses
RAG against documentation and source code, exposing the knowledge to Claude Desktop, Claude Code, Cursor, Windsurf or
any other MCP client.

It helped write this blog, and provided all the examples.

You can clone the [https://github.com/embabel/guide](https://github.com/embabel/guide) project and run it as a Spring
Boot application or via Docker. The README describes how to integrate it with Claude Desktop and popular coding agents.

This project will be the basis for a public chatbot that will help users learn and develop with Embabel.

### Other New Features

Among many other improvements:

* We’ve taken our first steps toward supporting the emerging [Agent Skills](https://agentskills.io/home) standard. See
  Embabel [documentation](https://docs.embabel.com/embabel-agent/guide/0.3.1-SNAPSHOT/#reference.agent_skills) for more
  information.
* We’ve added support for image generation and response streaming
* The Embabel [documentation](https://docs.embabel.com/embabel-agent/guide/0.3.1-SNAPSHOT/) is much improved

Looking Forward
---------------

We’re working towards a 1.0 GA release around late January.

> Embabel aims to make it impossible to justify choosing Python for production-grade agent systems not already committed
> to that ecosystem. The JVM is where most of the world’s valuable business logic runs. Gen AI enabling it matters.
> Embabel is the agent framework to unlock the enterprise value of Gen AI.

References
----------

* **Embabel Agent Framework**: [https://github.com/embabel/embabel-agent](https://github.com/embabel/embabel-agent)
* **Guide MCP Server and chatbot**: [https://github.com/embabel/guide](https://github.com/embabel/guide)
* **Ragbot chatbot**: [https://github.com/embabel/ragb](https://github.com/embabel/rag-demo)ot
* **Examples (Java & Kotlin)
  **: [https://github.com/embabel/embabel-agent-examples](https://github.com/embabel/embabel-agent-examples)
* **Documentation
  **: [https://docs.embabel.com/embabel-agent/guide/0.3.1-SNAPSHOT](https://docs.embabel.com/embabel-agent/guide/0.3.1-SNAPSHOT)
* **Discord**: Join our community Discord at [https://discord.gg/t6bjkyj93q](https://discord.gg/t6bjkyj93q)