#!/usr/bin/env python3
"""
MCP Advanced Features Integration Test

Tests 5 advanced FastMCP4J capabilities end-to-end from a real Python MCP
client over Streamable HTTP transport:

  1. Protocol notifications (progress + logging) via Context
  2. Resource templates (user://profile/{name})
  3. LLM sampling (ctx.sample)
  4. DI + lifespan (@McpInject, @McpOnStartup, @McpOnShutdown)
  5. Middleware

Server: AdvancedFeaturesServer (port 3003)
Transport: HTTP Streamable (bidirectional HTTP POST/SSE)
"""
import asyncio
import base64
import sys
from typing import Optional

from mcp import ClientSession
from mcp.client.streamable_http import streamable_http_client
from mcp.types import CreateMessageResult, TextContent


# Configuration
SERVER_URL: str = "http://127.0.0.1:3003/mcp"
MAX_RETRIES: int = 5
RETRY_DELAY: int = 2

# The message echoed back by the sampling callback. The server's sampledTool
# returns the text produced by the client's sampling callback, so we know
# sampling round-tripped correctly when this value shows up in the result.
SAMPLED_MESSAGE = "sampled-by-integration-test"


# Shared per-test notification captures (cleared before their specific test).
progress_events: list = []
log_events: list = []


def log_section(title: str) -> None:
    """Print a formatted section header."""
    print(f"\n{'=' * 60}")
    print(f"  {title}")
    print(f"{'=' * 60}")


def log_step(step: str) -> None:
    """Print a step indicator."""
    print(f"\n>>> {step}")


def log_result(test: str, result: str, success: bool = True) -> None:
    """Print a test result."""
    status = "✅" if success else "❌"
    print(f"  {status} {test}: {result}")


def _result_text(result) -> str:
    """Extract joined text from a CallToolResult, defensively."""
    parts = []
    for block in result.content:
        if hasattr(block, "text"):
            parts.append(block.text)
        else:
            parts.append(str(block))
    return "\n".join(parts)


def _resource_text(result) -> str:
    """Extract joined text from a ReadResourceResult, defensively.

    FastMCP4J marshals resources as base64 BlobResourceContents, while
    standards-compliant servers return TextResourceContents. Handle both.
    """
    if not getattr(result, "contents", None):
        return ""
    parts = []
    for content in result.contents:
        if getattr(content, "text", None):
            parts.append(content.text)
        elif getattr(content, "blob", None):
            try:
                parts.append(base64.b64decode(content.blob).decode("utf-8"))
            except Exception:
                parts.append(str(content))
        else:
            parts.append(str(content))
    return "\n".join(parts)


def _is_error(result) -> bool:
    return bool(getattr(result, "isError", False) or getattr(result, "is_error", False))


async def _sampling_cb(*args):
    """Answer sampling/createMessage with an echoed message.

    The callback signature varies across SDK versions:
      - modern (1.9+ / 2.x): async (context, params)
      - older: async (params)
    The CreateMessageRequestParams is always the LAST positional argument, so
    accepting *args keeps this robust across versions.
    """
    return CreateMessageResult(
        role="assistant",
        content=TextContent(type="text", text=SAMPLED_MESSAGE),
        model="integration-test-model",
    )


async def _logging_cb(params):
    """Record logging notifications (LoggingMessageNotificationParams)."""
    log_events.append(params)


async def _progress_cb(progress, total, message):
    """Record progress notifications (ProgressFnT: progress, total, message)."""
    progress_events.append((progress, total, message))


async def test_tools(session: ClientSession) -> bool:
    """Feature check: list tools and assert the expected names exist."""
    log_step("Listing available tools...")
    try:
        tools = await session.list_tools()
        names = {t.name for t in tools.tools}
        print(f"  Found {len(tools.tools)} tools: {sorted(names)}")

        expected = {"sampledTool", "progressEcho", "echo", "getInjectedValue", "startupFlag"}
        missing = expected - names
        success = not missing
        log_result("tool listing", f"missing={sorted(missing) if missing else 'none'}", success)
        return success
    except Exception as e:
        log_result("tool listing", f"ERROR: {e}", False)
        return False


async def test_resources(session: ClientSession) -> bool:
    """Feature 2: resource templates."""
    ok = True

    log_step("Listing resources...")
    try:
        resources = await session.list_resources()
        uris = {r.uri for r in resources.resources}
        has_info = "server://info" in uris
        log_result("list_resources includes server://info", str(has_info), has_info)
        ok = ok and has_info
    except Exception as e:
        log_result("list_resources", f"ERROR: {e}", False)
        ok = False

    log_step("Reading server://info (static resource)...")
    try:
        info_text = _resource_text(await session.read_resource("server://info"))
        has_info_content = "server-info" in info_text
        log_result("read server://info", repr(info_text), has_info_content)
        ok = ok and has_info_content
    except Exception as e:
        log_result("read server://info", f"ERROR: {e}", False)
        ok = False

    # Optional: list_resource_templates if the SDK exposes it (not required).
    log_step("Listing resource templates (best effort)...")
    try:
        if hasattr(session, "list_resource_templates"):
            templates = await session.list_resource_templates()
            template_uris = {t.uriTemplate for t in templates.resourceTemplates}
            has_profile = "user://profile/{name}" in template_uris
            log_result("list_resource_templates", str(sorted(template_uris)), has_profile)
            ok = ok and has_profile
        else:
            log_result("list_resource_templates", "not exposed by SDK (skipped)", True)
    except Exception as e:
        log_result("list_resource_templates", f"not supported: {e}", True)

    log_step("Reading user://profile/ford (template param binding)...")
    try:
        prof_text = _resource_text(await session.read_resource("user://profile/ford"))
        has_ford = "ford" in prof_text
        log_result("read user://profile/ford", repr(prof_text), has_ford)
        ok = ok and has_ford
    except Exception as e:
        log_result("read user://profile/ford", f"ERROR: {e}", False)
        ok = False

    return ok


async def test_sampling(session: ClientSession) -> bool:
    """Feature 3: LLM sampling via ctx.sample + client sampling_callback."""
    log_step("Testing LLM sampling (sampledTool)...")
    try:
        result = await session.call_tool("sampledTool", {"prompt": "hello-sampling"})
        text = _result_text(result)
        if _is_error(result):
            log_result("sampledTool", f"server error: {text}", False)
            return False
        success = SAMPLED_MESSAGE in text
        log_result(
            f"sampledTool echoes callback message '{SAMPLED_MESSAGE}'",
            repr(text),
            success,
        )
        return success
    except Exception as e:
        log_result("sampledTool", f"ERROR (sampling may be unsupported): {e}", False)
        return False


async def test_notifications(session: ClientSession) -> bool:
    """Feature 1: real protocol notifications (progress + logging)."""
    ok = True
    progress_events.clear()
    log_events.clear()

    # Opt into server logs so notifications/message is delivered to the
    # logging_callback on legacy (pre-2026) servers. Best effort.
    log_step("Setting logging level (best effort)...")
    try:
        await session.set_logging_level("debug")
        log_result("set_logging_level", "ok", True)
    except Exception as e:
        log_result("set_logging_level", f"skipped: {e}", True)

    log_step("Calling progressEcho with progress_callback...")
    try:
        result = await session.call_tool(
            "progressEcho",
            {"message": "x"},
            progress_callback=_progress_cb,
        )
        text = _result_text(result)
        if _is_error(result):
            log_result("progressEcho", f"server error: {text}", False)
            ok = False
        else:
            has_done = "done:x" in text
            log_result("progressEcho result", repr(text), has_done)
            ok = ok and has_done
    except Exception as e:
        log_result("progressEcho", f"ERROR: {e}", False)
        ok = False

    # Notifications are out-of-band; give the read loop a moment to flush.
    await asyncio.sleep(0.2)

    log_result(
        "progress notifications received",
        f"{len(progress_events)} events ({progress_events})",
        len(progress_events) > 0,
    )
    ok = ok and len(progress_events) > 0

    log_result(
        "logging notifications received",
        f"{len(log_events)} events",
        len(log_events) > 0,
    )
    ok = ok and len(log_events) > 0

    return ok


async def test_middleware(session: ClientSession) -> bool:
    """Feature 5: middleware appends ' [mw]' to the echo result."""
    log_step("Testing middleware (echo)...")
    try:
        result = await session.call_tool("echo", {"message": "hello"})
        text = _result_text(result)
        if _is_error(result):
            log_result("echo", f"server error: {text}", False)
            return False
        success = text.strip().endswith(" [mw]")
        log_result("echo passes through middleware", repr(text), success)
        return success
    except Exception as e:
        log_result("echo", f"ERROR: {e}", False)
        return False


async def test_di(session: ClientSession) -> bool:
    """Feature 4: @McpInject gives the injected config value."""
    log_step("Testing DI (getInjectedValue)...")
    try:
        result = await session.call_tool("getInjectedValue", {})
        text = _result_text(result)
        if _is_error(result):
            log_result("getInjectedValue", f"server error: {text}", False)
            return False
        success = "injected-secret-123" in text
        log_result("getInjectedValue returns injected secret", repr(text), success)
        return success
    except Exception as e:
        log_result("getInjectedValue", f"ERROR: {e}", False)
        return False


async def test_lifespan(session: ClientSession) -> bool:
    """Feature 4: @McpOnStartup set the startup flag."""
    log_step("Testing @McpOnStartup (startupFlag)...")
    try:
        result = await session.call_tool("startupFlag", {})
        text = _result_text(result)
        if _is_error(result):
            log_result("startupFlag", f"server error: {text}", False)
            return False
        success = text.strip() == "true"
        log_result("startupFlag reports true", repr(text), success)
        return success
    except Exception as e:
        log_result("startupFlag", f"ERROR: {e}", False)
        return False


async def run_test() -> bool:
    """Run all advanced-features tests against a single shared session."""
    log_section("MCP Advanced Features Integration Test")
    print(f"Target: {SERVER_URL}")
    print(f"Max retries: {MAX_RETRIES}")

    # list of (name, coroutine-func) executed in order on the shared session
    tests = [
        ("tools listing", test_tools),
        ("resources / templates", test_resources),
        ("llm sampling", test_sampling),
        ("protocol notifications", test_notifications),
        ("middleware", test_middleware),
        ("dependency injection", test_di),
        ("lifespan startup", test_lifespan),
    ]

    for attempt in range(1, MAX_RETRIES + 1):
        try:
            log_step(f"Connection attempt {attempt}/{MAX_RETRIES}...")

            async with streamable_http_client(SERVER_URL) as (read_stream, write_stream):
                async with ClientSession(
                    read_stream,
                    write_stream,
                    sampling_callback=_sampling_cb,
                    logging_callback=_logging_cb,
                ) as session:
                    log_result("Connection established", f"Connected to {SERVER_URL}")

                    await session.initialize()
                    print("  📋 Session initialized")

                    passed = 0
                    total = len(tests)
                    for name, fn in tests:
                        log_section(f"Test: {name}")
                        ok = await fn(session)
                        if ok:
                            passed += 1
                    failures = total - passed

                    if failures == 0:
                        log_section("✅ ALL TESTS PASSED")
                        return True

                    log_section(f"⚠️ {failures}/{total} TESTS FAILED")
                    return False

        except Exception as e:
            print(f"\n  ❌ Attempt {attempt} failed: {e}")
            if attempt < MAX_RETRIES:
                print(f"  ⏳ Retrying in {RETRY_DELAY}s...")
                await asyncio.sleep(RETRY_DELAY)

    log_section("❌ TESTS FAILED")
    print("Max retries exceeded. See error above.")
    import traceback
    traceback.print_exc()
    return False


def main() -> int:
    """Entry point."""
    try:
        result = asyncio.run(run_test())
        return 0 if result else 1
    except KeyboardInterrupt:
        print("\n\n⚠️  Test interrupted by user")
        return 130
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
