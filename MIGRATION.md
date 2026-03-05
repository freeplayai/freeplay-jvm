# Migrating from the Thick Client to the Thin Client

Version 0.5.0 removes the original ("thick") Freeplay client and promotes the thin client as the
only interface. This guide shows the key before/after patterns.

---

## 1. Initialization

**Before:**
```java
import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.ProviderConfigs;

Freeplay fp = new Freeplay(
    ProviderConfigs.anthropic(anthropicApiKey),
    freeplayApiKey,
    projectId
);
```

**After:**
```java
import ai.freeplay.client.Freeplay;
import static ai.freeplay.client.Freeplay.Config;

Freeplay fp = new Freeplay(Config()
    .freeplayAPIKey(freeplayApiKey)
    .customerDomain(customerDomain)
);
```

The thin client no longer deals with provider credentials since your code makes those calls.

---

## 2. Getting a formatted prompt and calling the LLM

**Before** (thick client managed the LLM call internally):
```java
ChatSession session = fp.startChat(
    "my-prompt",
    "latest",
    variables,
    AnthropicChatFlavor.INSTANCE
);
String reply = session.chat("Why isn't my window working?");
```

**After** (your code fetches the prompt, calls the LLM, then records the results):
```java
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.FormattedPrompt;

FormattedPrompt<List<ChatMessage>> formatted = fp.prompts()
    .<List<ChatMessage>>getFormatted(projectId, "my-prompt", "latest", variables)
    .get();

// Call your LLM of choice using formatted.getFormattedPrompt() as the messages,
// formatted.getPromptInfo().getModel() as the model, etc.
```

You can use one of the provided adapters to convert the formatted prompt to the shape your LLM SDK expects:

```java
import ai.freeplay.client.adapters.LLMAdapters;
import ai.freeplay.client.adapters.AnthropicLLMAdapter;

AnthropicLLMAdapter adapter = (AnthropicLLMAdapter) LLMAdapters.adapterForFlavor("anthropic_chat");
// adapter.formatMessages(...), adapter.formatTools(...), etc.
```

---

## 3. Recording completions

**Before** (recording was automatic):
```java
// The thick client recorded automatically when you called session.chat(...)
```

**After** (recording is explicit):
```java
import ai.freeplay.client.resources.recordings.CallInfo;
import ai.freeplay.client.resources.recordings.RecordInfo;
import ai.freeplay.client.resources.recordings.ResponseInfo;

long start = System.currentTimeMillis();
// ... make your LLM call ...
long end = System.currentTimeMillis();

List<ChatMessage> allMessages = formatted.allMessages(
    new ChatMessage("assistant", llmResponseText)
);

fp.recordings().create(
    new RecordInfo(projectId, allMessages)
        .inputs(variables)
        .promptVersionInfo(formatted.getPromptInfo())
        .callInfo(CallInfo.from(formatted.getPromptInfo(), start, end))
        .responseInfo(new ResponseInfo(false))
).get();
```

---

## 4. Sessions

**Before:**
```java
// Sessions were managed internally by the thick client's ChatSession
```

**After:**
```java
import ai.freeplay.client.resources.sessions.SessionInfo;

SessionInfo session = fp.sessions().create().getSessionInfo();

fp.recordings().create(
    new RecordInfo(projectId, allMessages)
        .sessionInfo(session)
        // ... other fields
).get();
```

---

## 5. Customer feedback

**Before:**
```java
import ai.freeplay.client.CompletionFeedback;
fp.submitFeedback(completionId, CompletionFeedback.POSITIVE_FEEDBACK);
```

**After:**
```java
fp.customerFeedback().update(
    projectId,
    completionId,
    Map.of("helpful", "thumbsup")
).get();
```

---

## 6. Package changes summary

| Old import | New import |
|---|---|
| `ai.freeplay.client.thin.Freeplay` | `ai.freeplay.client.Freeplay` |
| `ai.freeplay.client.thin.resources.prompts.*` | `ai.freeplay.client.resources.prompts.*` |
| `ai.freeplay.client.thin.resources.recordings.*` | `ai.freeplay.client.resources.recordings.*` |
| `ai.freeplay.client.thin.resources.sessions.*` | `ai.freeplay.client.resources.sessions.*` |
| `ai.freeplay.client.thin.resources.feedback.*` | `ai.freeplay.client.resources.feedback.*` |
| `ai.freeplay.client.thin.resources.metadata.*` | `ai.freeplay.client.resources.metadata.*` |
| `ai.freeplay.client.thin.OpenAILLMAdapter` | `ai.freeplay.client.adapters.OpenAILLMAdapter` |
| `ai.freeplay.client.thin.AnthropicLLMAdapter` | `ai.freeplay.client.adapters.AnthropicLLMAdapter` |
| `ai.freeplay.client.thin.GeminiLLMAdapter` | `ai.freeplay.client.adapters.GeminiLLMAdapter` |
| `ai.freeplay.client.thin.GeminiApiLLMAdapter` | `ai.freeplay.client.adapters.GeminiApiLLMAdapter` |
| `ai.freeplay.client.thin.BedrockConverseAdapter` | `ai.freeplay.client.adapters.BedrockConverseAdapter` |
| `ai.freeplay.client.thin.LLMAdapters` | `ai.freeplay.client.adapters.LLMAdapters` |
