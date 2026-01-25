# MCP Test Client

A comprehensive Python test client for MCP (Model Context Protocol) localhost-server tools.

## Features

- Tests all 7 MCP tools: `calculate`, `echo`, `fileread`, `filewrite`, `memory`, `todo`, `planner`
- 10+ test cases per tool (70+ total tests)
- Color-coded terminal output
- JSON result export
- Async/await support

## Installation

```bash
# Install dependencies
pip install -r requirements.txt
```

## Usage

### Run All Tests
```bash
python mcp_test_client.py
```

### Run Specific Tool Tests
```bash
python mcp_test_client.py --tool todo
python mcp_test_client.py --tool planner
python mcp_test_client.py --tool calculate
```

### With Custom Server Command
```bash
python mcp_test_client.py --server java -cp path/to/fastmcp4j.jar com.ultrathink.fastmcp.Main
```

## Test Coverage

| Tool | Tests | Coverage |
|------|-------|----------|
| calculate | 10 | All operations (ADD, SUBTRACT, MULTIPLY, DIVIDE) |
| echo | 10 | Messages, special chars, unicode, emojis, multiline |
| fileread | 10 | getStats, readFile, readLines, grep (all modes) |
| filewrite | 10 | writeFile, appendFile, writeLines, appendLines, deleteFile, createDirectory |
| memory | 10 | create, view, str_replace, insert, delete, rename |
| todo | 10 | add, list, update, delete, clearCompleted, priorities, sorting |
| planner | 10 | createPlan, listPlans, getPlan, addTask, addSubtask, updateTask, deletePlan |

## Output

Results are saved to: `test_results_YYYYMMDD_HHMMSS.json`

Example output:
```
============================================================
Testing CALCULATE Tool (10 tests)
============================================================
[PASS] calculate:ADD - ADD positive
[PASS] calculate:ADD - ADD negative
[PASS] calculate:SUBTRACT - SUBTRACT
...
```

## Requirements

- Python 3.8+
- MCP server running (e.g., fastmcp4j)
