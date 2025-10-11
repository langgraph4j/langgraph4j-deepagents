# 🧠🤖Deep Agents for java

Using an LLM to call tools in a loop is the simplest form of an agent. This architecture, however, can yield agents that are "shallow" and fail to plan and act over longer, more complex tasks. Applications like "Deep Research", "Manus", and "Claude Code" have gotten around this limitation by implementing a combination of four things: a planning tool, sub agents, access to a file system, and a detailed prompt.

`deepagents` is a Java package that implements these in a general purpose way so that you can easily create a Deep Agent for your application.

> It is inpired by the Python version. See [here: hwchase17/deepagents](https://github.com/hwchase17/deepagents)


## Learn more

For more information, check out our docs: [https://docs.langchain.com/labs/deep-agents/overview](https://docs.langchain.com/labs/deep-agents/overview)
