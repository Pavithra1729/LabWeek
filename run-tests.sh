#!/usr/bin/env bash
# =============================================================================
# BugBot end-to-end test automation (sample mode — no Jira account required).
# Starts the backend, exercises the dashboard, MCP server, and ChatOps,
# prints results + server logs, then shuts the server down.
# =============================================================================
set -uo pipefail

BASE="http://localhost:8080"
LOG="/tmp/bugbot.log"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

pass=0; fail=0
hr() { printf '%s\n' "----------------------------------------------------------------"; }
check() { # check "label" "expected-substring" "actual"
  if printf '%s' "$3" | grep -q "$2"; then
    echo "  ✅ PASS: $1"; pass=$((pass+1))
  else
    echo "  ❌ FAIL: $1 (expected to contain '$2')"; echo "     got: $3"; fail=$((fail+1))
  fi
}

cleanup() { lsof -ti:8080 2>/dev/null | xargs kill -9 2>/dev/null; }
trap cleanup EXIT

echo "### Ensuring sample mode (no Jira token) and free port ###"
unset BUGBOT_JIRA_APITOKEN
cleanup; sleep 1

echo "### Starting BugBot backend... ###"
cd "$PROJECT_DIR"
( mvn -q -DskipTests spring-boot:run > "$LOG" 2>&1 & )

echo -n "Waiting for startup"
up=""
for i in $(seq 1 90); do
  if curl -sf "$BASE/api/dashboard" >/dev/null 2>&1; then up="yes"; echo " -> UP after ${i}s"; break; fi
  echo -n "."; sleep 1
done
if [ -z "$up" ]; then
  echo " -> FAILED TO START. Last 30 log lines:"; tail -30 "$LOG"; exit 1
fi

hr; echo "TEST 1 — Dashboard (standup summary + role groups)"; hr
DASH=$(curl -s "$BASE/api/dashboard")
echo "$DASH" | python3 -m json.tool | head -14
check "dashboard returns 7 active sample tickets" '"totalActive":7' "$(echo "$DASH" | tr -d ' ')"
check "standup summary present" 'high-severity' "$DASH"
check "VS Code deep link generated" 'vscode://file' "$DASH"

hr; echo "TEST 2 — MCP tools/list"; hr
TOOLS=$(curl -s "$BASE/mcp" -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}')
echo "$TOOLS" | python3 -c "import sys,json;[print('  -',t['name']) for t in json.load(sys.stdin)['result']['tools']]"
check "jira.search advertised" 'jira.search' "$TOOLS"
check "jira.transition advertised" 'jira.transition' "$TOOLS"

hr; echo "TEST 3 — MCP tools/call jira.search"; hr
SEARCH=$(curl -s "$BASE/mcp" -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"jira.search","arguments":{"jql":"statusCategory != Done","maxResults":5}}}')
echo "  $(echo "$SEARCH" | python3 -c "import sys,json;print('returned count =', json.load(sys.stdin)['result']['structuredContent']['count'])")"
check "search returned results" '"count":5' "$(echo "$SEARCH" | tr -d ' ')"

hr; echo "TEST 4 — ChatOps: status QA-101"; hr
S=$(curl -s "$BASE/api/messages" -H 'content-type: application/json' \
  -d '{"type":"message","text":"@BugBot status QA-101"}')
echo "  $(echo "$S" | python3 -c "import sys,json;print(json.load(sys.stdin)['text'].splitlines()[0])")"
check "status returns QA-101" 'QA-101' "$S"

hr; echo "TEST 5 — ChatOps: update QA-101 -> Done"; hr
U=$(curl -s "$BASE/api/messages" -H 'content-type: application/json' \
  -d '{"type":"message","text":"@BugBot update QA-101 Done"}')
echo "  $(echo "$U" | python3 -c "import sys,json;print(json.load(sys.stdin)['text'])")"
check "transition acknowledged" 'Moved' "$U"

hr; echo "TEST 6 — ChatOps: comment QA-102"; hr
C=$(curl -s "$BASE/api/messages" -H 'content-type: application/json' \
  -d '{"type":"message","text":"@BugBot comment QA-102 Fixed in PR #501, please retest"}')
echo "  $(echo "$C" | python3 -c "import sys,json;print(json.load(sys.stdin)['text'])")"
check "comment acknowledged" 'Comment added' "$C"

hr; echo "TEST 7 — Verify QA-101 dropped from active board (7 -> 6)"; hr
D2=$(curl -s "$BASE/api/dashboard")
echo "  totalActive now = $(echo "$D2" | python3 -c "import sys,json;print(json.load(sys.stdin)['totalActive'])")"
check "active count dropped to 6" '"totalActive":6' "$(echo "$D2" | tr -d ' ')"
check "QA-101 no longer on board" 'absent' "$(echo "$D2" | grep -q 'QA-101' && echo present || echo absent)"

hr; echo "RESULTS: ${pass} passed, ${fail} failed"; hr

echo; echo "### Server log tail (startup banner / Tomcat port) ###"
grep -E "Started BugBotApplication|Tomcat started|sample|Mapped|LiveReload" "$LOG" | tail -15
echo; echo "(full log at $LOG)"

exit $fail

