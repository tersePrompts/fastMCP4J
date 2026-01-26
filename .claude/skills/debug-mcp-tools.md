---
description: Run smoke test on all 7 MCP localhost-server tools
---

# MCP Smoke Test

Runs a quick smoke test on all MCP localhost-server tools to verify they're working.

## Tests Performed

1. **calculate** - Test addition (10 + 5 = 15)
2. **echo** - Test echo with "Smoke test" message
3. **fileread** - Test readFile mode on a test file
4. **filewrite** - Test writing a file
5. **memory** - Test creating a memory entry
6. **todo** - Test adding a high priority todo
7. **planner** - Test creating a plan

## Usage

Run this skill to quickly verify MCP server connectivity and functionality.

## Expected Output

- 6/7 tools should pass (calculate, echo, fileread, filewrite, memory, todo, planner)
- Results show pass/fail for each tool
