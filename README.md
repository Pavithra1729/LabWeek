# 🐞 BugBot — Advanced LLM + MCP Jira/Teams Dashboard (Reference Implementation)

A reference implementation of **Option B: The Advanced LLM & MCP Approach**.
BugBot turns raw Jira tickets into a smart, interactive **Daily Standup**
inside Microsoft Teams, with contextual **jump-to-code** links into VS Code and
**ChatOps** commands backed by an **MCP** server.

```
 Microsoft Teams Tab (React)  ─┐
                               ├──▶  BugBot Spring Boot backend  ──▶ Jira Cloud REST
 Teams Bot (@BugBot ChatOps)  ─┘        │  ├─ Jira MCP server (JSON-RPC tools)
                                        │  ├─ LLM pipeline (standup summary)
                                        │  └─ Adaptive Cards + VS Code deep links
 MCP-capable LLM client  ───────────────┘  (POST /mcp)
```

## What each feature maps to

| Feature (from the brief)            | Where it lives |
|-------------------------------------|----------------|
| "Daily Standup" summary widget      | `llm/StandupSummaryService.java` + `dashboard/DashboardService.java` → `GET /api/dashboard` |
| Grouping by role (Tester vs Dev)    | `DashboardService#roleOf` |
| Contextual jump links to VS Code    | `teams/DeepLinkService.java` (`vscode://...`) |
| Interactive ChatOps (`@BugBot ...`) | `teams/ChatOpsService.java` + `teams/BotController.java` |
| Expose Jira via MCP                 | `mcp/McpServerController.java` + `mcp/JiraMcpTools.java` → `POST /mcp` |
| Teams App/Tab widget                | `teamsapp/` (manifest + React tab) |
| Adaptive Cards rendering            | `teams/AdaptiveCardBuilder.java` → `GET /api/dashboard/card` |

## 0. Quick test with sample bugs (no Jira account needed)

BugBot ships with a **sample mode** that activates automatically whenever no
real Jira API token is configured (`JiraProperties.sampleMode()`). It serves
7 realistic in-memory bugs (`jira/SampleBugStore.java`) — high-severity
regressions, code-review items, flaky tests — each with file references so the
VS Code deep links resolve. ChatOps `update`/`comment` mutate this in-memory
data, so you can see state changes on the dashboard.

```bash
# Make sure NO Jira token is set (sample mode on):
unset BUGBOT_JIRA_APITOKEN
mvn spring-boot:run
```

Then in another terminal:

```bash
# 1) Dashboard: 3-bullet standup summary + tickets grouped by role
curl -s localhost:8080/api/dashboard | python3 -m json.tool

# 2) MCP server: list the Jira tools exposed to an LLM
curl -s localhost:8080/mcp -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | python3 -m json.tool

# 3) MCP tool call: search sample bugs
curl -s localhost:8080/mcp -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"jira.search","arguments":{"jql":"statusCategory != Done","maxResults":5}}}' | python3 -m json.tool

# 4) ChatOps: read a status
curl -s localhost:8080/api/messages -H 'content-type: application/json' \
  -d '{"type":"message","text":"@BugBot status QA-101"}'

# 5) ChatOps: transition a ticket (then re-fetch /api/dashboard to see it drop
#    off the active board as its status becomes Done)
curl -s localhost:8080/api/messages -H 'content-type: application/json' \
  -d '{"type":"message","text":"@BugBot update QA-101 Done"}'

# 6) ChatOps: add a comment
curl -s localhost:8080/api/messages -H 'content-type: application/json' \
  -d '{"type":"message","text":"@BugBot comment QA-102 Fixed in PR #501, please retest"}'
```

Sample tickets you can target in ChatOps: `QA-101`, `QA-102`, `QA-103`,
`DEV-201`, `DEV-202`, `DEV-203`, `DEV-204`.

To view the sample data in the React tab, run the backend as above and start
the tab (`cd teamsapp/tab && npm install && npm run dev`), then open
<http://localhost:53000>.

> Turn sample mode **off** simply by setting a real `BUGBOT_JIRA_APITOKEN`.

## 1. Run the backend (the "brain")

```bash
# set secrets via env vars (never commit these)
export BUGBOT_JIRA_BASEURL="https://your-org.atlassian.net"
export BUGBOT_JIRA_EMAIL="you@your-org.com"
export BUGBOT_JIRA_APITOKEN="<atlassian-api-token>"
export BUGBOT_JIRA_PROJECT="QA"

# optional: turn on the real LLM summary (otherwise a deterministic fallback is used)
export BUGBOT_LLM_ENABLED="true"
export BUGBOT_LLM_APIKEY="<openai-or-azure-key>"
export BUGBOT_LLM_MODEL="gpt-4o-mini"

# for the VS Code jump links
export BUGBOT_REPO_ROOT="vscode://file//Users/you/project"

mvn spring-boot:run
```

Key endpoints:

- `GET  /api/dashboard`       → structured JSON for the React tab
- `GET  /api/dashboard/card`  → Adaptive Card JSON (post into a channel)
- `POST /api/messages`        → Teams bot webhook (ChatOps)
- `POST /mcp`                 → MCP JSON-RPC server (Jira tools)

### Try the MCP server with curl

```bash
# list tools
curl -s localhost:8080/mcp -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | jq

# call a tool
curl -s localhost:8080/mcp -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call",
       "params":{"name":"jira.search","arguments":{"jql":"statusCategory != Done","maxResults":10}}}' | jq
```

### Try ChatOps

```bash
curl -s localhost:8080/api/messages -H 'content-type: application/json' \
  -d '{"type":"message","text":"@BugBot update JIRA-123 In Review"}' | jq
```

## 2. Run the Teams Tab (React)

```bash
cd teamsapp/tab
npm install
npm run dev     # serves on http://localhost:53000 (CORS-allowed by the backend)
```

Open <http://localhost:53000> in a browser to see the dashboard, or sideload it
into Teams using the manifest in `teamsapp/appPackage/`.

## 3. Package & sideload into Teams

1. Install the **Teams Toolkit** extension in VS Code.
2. Edit `teamsapp/appPackage/manifest.json`:
   - `id` → a new GUID for the app
   - `bots[0].botId` → your Azure Bot app id
   - `staticTabs[0].contentUrl` and `validDomains` → your tab host
3. Zip `manifest.json` + `color.png` + `outline.png` and upload via
   *Teams → Apps → Manage your apps → Upload a custom app*.

## Connecting an MCP-capable LLM client

Any MCP client (VS Code MCP, Claude Desktop, a custom agent) can talk to the
`POST /mcp` endpoint. Example client config (HTTP transport):

```jsonc
{
  "servers": {
    "bugbot-jira": { "url": "http://localhost:8080/mcp" }
  }
}
```

The model then has secure, typed access to exactly four tools:
`jira.search`, `jira.getComments`, `jira.addComment`, `jira.transition`.

## Agentic ChatOps (next step)

`ChatOpsService` uses a deterministic parser for safety. To make it fully
natural-language, forward the user's message to the LLM with the MCP tools
exposed, and let the model decide which tool to call. The MCP server already
enforces the security boundary, so the model can only perform the four
whitelisted Jira actions.

## Production hardening checklist

- [ ] Validate Bot Framework JWT on `/api/messages`.
- [ ] Protect `/mcp` with auth (API key / OAuth) and per-tool authorization.
- [ ] Lock CORS to your Teams tenant only.
- [ ] Add caching/rate-limiting on Jira calls.
- [ ] Store secrets in a vault (Azure Key Vault), not env files.

