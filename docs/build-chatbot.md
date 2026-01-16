Building a Chatbot with Embabel Agentic RAG
===========================================

[![Rod Johnson](https://miro.medium.com/v2/resize:fill:64:64/0*w7FhiB6igokPDlHh.jpeg)](https://medium.com/@springrod?source=post_page---byline--b26a8346cb16---------------------------------------)

[Rod Johnson](https://medium.com/@springrod?source=post_page---byline--b26a8346cb16---------------------------------------)

7 min read

·

1 day ago

[nameless link](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2F_%2Fvote%2Fp%2Fb26a8346cb16&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40springrod%2Fbuilding-a-chatbot-with-embabel-agentic-rag-b26a8346cb16&user=Rod+Johnson&userId=de5947672915&source=---header_actions--b26a8346cb16---------------------clap_footer------------------)

--

1

[nameless link](https://medium.com/m/signin?actionUrl=https%3A%2F%2Fmedium.com%2F_%2Fbookmark%2Fp%2Fb26a8346cb16&operation=register&redirect=https%3A%2F%2Fmedium.com%2F%40springrod%2Fbuilding-a-chatbot-with-embabel-agentic-rag-b26a8346cb16&source=---header_actions--b26a8346cb16---------------------bookmark_footer------------------)

Listen

Share

In my [last blog](https://medium.com/p/rethinking-rag-pipelines-are-the-past-agentic-is-the-future), I covered the
advantages of agentic RAG over legacy pipelines. Let’s see how to build an agentic RAG chatbot with Embabel. You can
find the complete code in the [Ragbot repository](https://github.com/embabel/ragbot).

Responding to Messages
----------------------

### Chat Actions

In Embabel there’s nothing special about a chatbot. It’s backed by Embabel’s core `AgentProcess`—a long-running process
that can receive messages and respond to them over time. Thus the same action-based architecture that handles business
workflows also handles chat interactions. Chat processes have have a `Conversation` object in their blackboard, but
nothing else about them is special. (Remember that Embabel doesn’t otherwise need to maintain a conversation thread,
because it emphasizes structure over loose text.) Chat processes can also respond to incoming events other than user
messages, which can help inform the conversation.

A chatbot action responds to an incoming `UserMessage` and sends one or more assistant messages. The simplest possible
example:

```
@Action(canRerun = true, trigger = UserMessage.class)
void respond(Conversation conversation, ActionContext context) {
    var assistantMessage = context
      .ai()
       withAutoLlm()
      .withSystemPrompt("You are a helpful assistant")
      .respond(conversation.getMessages());
    context.sendMessage(conversation.addMessage(assistantMessage));
}
```

The `@Action` annotation with `trigger = UserMessage.class` means this action fires whenever a user message arrives. The
`canRerun = true` flag allows it to run repeatedly. The `ActionContext` parameter provides access to the LLM through
`context.ai()`, as well as the ability to send messages.

So little code looks cute, but we want something useful, rather than the millionth and first “you are a helpful
assistant” toy demo.

Here’s the entire `ChatActions` class from Ragbot, with a realistic chat action:

```
@EmbabelComponent
public class ChatActions {
    private final ToolishRag toolishRag;
    private final RagbotProperties properties;
    public ChatActions(
            SearchOperations searchOperations,
            RagbotProperties properties) {
        this.toolishRag = new ToolishRag(
                "sources",
                "Music criticism",
                searchOperations);
        this.properties = properties;
    }
    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(Conversation conversation, ActionContext context) {
        var assistantMessage = context
                .ai()
                .withLlm(properties.chatLlm())
                .withReference(toolishRag)
                .withTemplate("ragbot")
                .respondWithSystemPrompt(conversation, Map.of(
                        "properties", properties,
                        "voice", properties.voice(),
                        "objective", properties.objective()
                ));
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
```

The key additions are the use of a prompt template, and the call to `.withReference(toolishRag)`. This gives the LLM
access to RAG tools. The LLM decides when and how to search—true agentic retrieval rather than a blind pipeline.

### Managing Prompts with Templates

Chatbot prompts tend to grow complex. You need persona definitions, guardrails, response guidelines, objective
instructions. Embedding all this even in multi-line strings is painful.

When prompts get complex, Embabel makes it easy to use [Jinja](https://jinja.palletsprojects.com/en/stable/) templates
to externalize them. The main Ragbot template is built from reusable elements:

```
{% include "elements/guardrails.jinja" %}
{% include "elements/personalization.jinja" %}
Keep your responses under {{ properties.voice().maxWords() }} words unless they
MUST be longer for a detailed response or quoting content.
```

The personalization template dynamically loads persona and objective:

```
============
# Important: Your voice
{% set persona_template = "personas/" ~ properties.voice().persona() ~ ".jinja" %}
{% include persona_template %}
============
============
# Important: Your objectives
{% set objective_template = "objectives/" ~ properties.objective() ~ ".jinja" %}
{% include objective_template %}
============
```

The separation is useful. **Voice** defines _how_ the chatbot communicates — its personality and tone. **Objective**
defines _what_ it should accomplish — the domain expertise and task instructions. You can mix and match: the same legal
objective answered in the voice of Shakespeare, a sarcastic teenager, or a formal lawyer.

Here’s a simple persona (`personas/music-guide.jinja`):

```
You are Sforzando, an enthusiastic guide to classical music.
You want to share your passion for this genre with others.
Your tone is friendly and engaging. You are not formal or academic. 
You want to make classical music accessible and enjoyable for everyone.
```

An objective (`objectives/music.jinja`):

```
Answer questions about music critics in a clear and concise manner.
The tools available to you look into the archives of critics.
You must always use these tools to find answers, as your general knowledge 
will not extend to everything they wrote.
Always back up your points with direct quotes from the criticism.
DO NOT RELY ON GENERAL KNOWLEDGE unless you are certain a better answer 
is not in the provided sources.
```

Ragbot allows switching personas in `application.yml` without touching code:

```
ragbot:
  voice:
    persona: shakespeare  # Now responds in Elizabethan English
    max-words: 250
  objective: music
```

### Configuring a Chatbot

So that our action(s) are invoked, we need to configure a `Chatbot`.

Embabel’s [**Utility AI**](https://en.wikipedia.org/wiki/Utility_system) planning type is ideal for chatbots. Utility AI
scores available actions and selects the most appropriate one for each situation. (An action can never be invoked unless
its type requirements and other preconditions are satisfied, so type safety remains guaranteed.) For a simple chatbot
with a single action, scoring isn’t needed. But it means you can easily add more actions later without restructuring
your code. When a user message comes in, the platform will choose the highest value action at that moment.

We define a Spring bean that uses the `AgentProcessChat` helper class to create a utility chatbot:

```
@Configuration
class ChatConfiguration {
    @Bean
    Chatbot chatbot(AgentPlatform agentPlatform) {
        return AgentProcessChatbot.utilityFromPlatform(agentPlatform);
    }
}
```

The `utilityFromPlatform()` method automatically discovers all `@Action` methods in your `@EmbabelComponent` classes.
Your chatbot can gain capabilities just by adding new actions.

Configuring SearchOperations to back RAG
----------------------------------------

We need to define the `SearchOperations` instance injected into `ChatActions` and provided to `ToolishRag`. Here we use
the `LuceneSearchOperations` fluent API:

```
@Configuration
@EnableConfigurationProperties(RagbotProperties.class)
class RagConfiguration {
    @Bean
    LuceneSearchOperations luceneSearchOperations(
            ModelProvider modelProvider,
            RagbotProperties properties) {
        var embeddingService = modelProvider.getEmbeddingService(
            DefaultModelSelectionCriteria.INSTANCE);
        return LuceneSearchOperations
                .withName("docs")
                .withEmbeddingService(embeddingService)
                .withChunkerConfig(properties.chunkerConfig())
                .withChunkTransformer(AddTitlesChunkTransformer.INSTANCE)
                .withIndexPath(Paths.get("./.lucene-index"))
                .buildAndLoadChunks();
    }
}
```

This creates a Lucene-backed text and vector store that persists to disk. The `LuceneSearchOperations` implements our
`SearchOperations` interface, which `ToolishRag` wraps to expose fine-grained search tools to the LLM.

Running The App
---------------

Ragbot starts with Spring Shell.

Run `./scripts.shell.sh` from the command line, or run `RagbotApplication` from your IDE.

Once it starts up, we need to ingest data, which will survive restarts.

The sample data is 19th and early 20th century music criticism. An obscure choice helps prove that the LLM is not just
relying on general knowledge, which can be hard to do with familiar content. It also reflects the fact that I have a PhD
on Romantic piano music, and am currently listening
to [Glazunov’s wonderful violin concerto](https://www.youtube.com/watch?v=jq8oxIgyp8E).

To use the sample data, begin by running `ingest`, which will default to ingesting Robert Schumann’s music criticism.
Then ingest the Project Gutenberg URL of Philip Hale’s legendary early 20th century program notes. Run these commands in
the shell:

```
> ingest
> ingest https://www.gutenberg.org/files/56208/56208-h/56208-h.htm
```

You can also ingest your own content using the shell. See the [Ragbot README](https://github.com/embabel/ragbot) for
documentation.

You can now type `chat` in the shell for text-based chat, but a UI is more fun. I got Claude to produce a
simple [Javelit](https://javelit.io) web interface for Ragbot. The `uichat` shell command launches it. The port defaults
to `8888`but you can specify your own:

```
> uichat
Web UI started at http://localhost:8888
```

You can now chat and ask questions. The LLM will use the RAG tools to search the indexed content and provided grounded
responses. You’ll see the tool calls as they are made.

![captionless image](https://miro.medium.com/v2/resize:fit:1400/format:webp/0*HkORHxKZvep53EL_.jpeg)

To demonstrate Ragbot’s understanding of the sources, try questions like:

* _What did Schumann think of Liszt?_
* _What did Philip Hale think of Liszt?_
* _Contrast Hale and Schumann’s attitude to Liszt?_

The latter is difficult for traditional pipeline RAG, but not agentic RAG.

Store Choice
------------

Embabel’s RAG module supports multiple stores through the `SearchOperations` interface. Currently we support:

**Apache Lucene** — In-memory or disk-persisted vector storage. No external dependencies. Good for development and
smaller datasets.

**Neo4j** — Graph database with vector indices. Enables entity relationships and document structure navigation alongside
vector search.

**Spring AI VectorStore** — Adapts any Spring AI vector store (pgvector, Pinecone, Chroma, etc.) to work with Embabel’s
agentic RAG. _Does not support full text search or structure, which limits it usefulness._

**pgvector** — coming soon.

The `SearchOperations` interfaces are simple, so it’s easy to implement your own search and benefit from the same
programming model.

The API remains consistent, regardless of the backend. However, the store affects which tools `ToolishRag` exposes.
Lucene gets vector search, text search, regex, and chunk expansion. Neo4j adds document structure navigation. Spring AI
adapters provide basic vector search. The LLM always gets the best available toolkit for your store.

Ingesting Data
--------------

Ragbot includes shell commands for ingestion. Like the API, ingestion is also independent of the backing store.

Embabel RAG uses [Apache Tika](https://tika.apache.org/) by default for content extraction:

```
var ingested = NeverRefreshExistingDocumentContentPolicy.INSTANCE
        .ingestUriIfNeeded(
                luceneSearchOperations,
                new TikaHierarchicalContentReader(),
                uri
        );
```

Tika handles HTML, PDF, Word documents, and dozens of other formats. It extracts hierarchical structure — titles,
sections, paragraphs — which Embabel preserves in the index.

For complex documents that Tika doesn’t handle well, [docling](https://ds4sd.github.io/docling/) is a solid choice. It
converts PDFs, Word docs, and PowerPoints to clean Markdown that ingests reliably. Preprocessing with docling, then
ingesting the markdown, often produces better results than direct ingestion of complex formats.

> **_Pipeline architectures are fine for ingestion._** _An agentic approach is essential at at query time, when the LLM
needs to reason about what to retrieve. Ingestion is a batch process where predetermined steps make sense. Embabel
offers a RAG pipeline where chunks can be transformed — for example, to add information about the documents and sections
they were taken from, as Ragbot does._

Exploring This Yourself
-----------------------

The complete example is available at [github.com/embabel/ragbot](https://github.com/embabel/ragbot). Clone it, set your
API key, and you’ll have a working agentic RAG chatbot in minutes. Change the prompt templates and ingest your own data.

The architecture, and agentic RAG, generalizes beyond chatbots. Any Embabel agent can use `ToolishRag` to search and
reason about documents. The same pattern works for document analysis workflows, research assistants, compliance
checkers, and anywhere you need grounded LLM responses.

To start from scratch with your own Embabel project, create a new Embabel repository from
the [Java agent template](https://github.com/embabel/java-agent-template) GitHub template repository.

Coming Soon
-----------

I’m working on [**Impromptu**](https://github.com/embabel/impromptu) — a full-featured classical music exploration
application that integrates with YouTube, Spotify, Wikipedia and other sources. It combines agentic RAG with entity
extraction, knowledge graphs and sophisticated memory system for a comprehensive music research and discovery platform.

Watch this space.