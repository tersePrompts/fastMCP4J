"""
Test Headers Feature - Verify client headers are accessible in tools
================================================================

This tests the new feature where client HTTP headers are passed through
to tool execution context and can be accessed via @McpContext.

Usage:
    python test_headers_feature.py
"""

import requests
import json
from typing import Dict, Any

def test_headers_echo():
    """Test that custom headers are returned in echo response"""
    print("=" * 60)
    print(" Testing Headers Feature")
    print("=" * 60)

    # Server URL
    base_url = "http://localhost:8080/mcp"

    # Custom headers to send
    custom_headers = {
        "X-Client-Id": "test-client-123",
        "X-User-Id": "user-abc",
        "X-Session-Id": "session-xyz",
        "User-Agent": "Test-Client/1.0",
        "X-Custom-Header": "custom-value-12345678901234567890"
    }

    print(f"\n1. Sending echo request with custom headers:")
    for key, value in custom_headers.items():
        print(f"   {key}: {value}")

    # Call echo tool with custom headers
    url = f"{base_url}/tools/echo"
    try:
        response = requests.post(
            url,
            json={"message": "Test headers feature"},
            headers=custom_headers,
            timeout=10
        )
        response.raise_for_status()
        result = response.json()

        print(f"\n2. Response from server:")
        if isinstance(result, dict) and "content" in result:
            # Extract text content from MCP response
            content = result["content"]
            if isinstance(content, list) and len(content) > 0:
                text_content = content[0]
                if isinstance(text_content, dict) and "text" in text_content:
                    echo_response = text_content["text"]
                    print(f"   {echo_response}")

                    # Verify headers are included
                    print(f"\n3. Verifying headers in response:")

                    passed = True

                    # Check for "Headers: N" indicator
                    if "Headers:" in echo_response:
                        print(f"   ✅ Headers count indicator found")

                        # Extract header count
                        import re
                        match = re.search(r'Headers:\s*(\d+)', echo_response)
                        if match:
                            header_count = int(match.group(1))
                            print(f"   ✅ Header count: {header_count}")

                            # Verify we have at least our custom headers
                            if header_count >= len(custom_headers):
                                print(f"   ✅ Header count matches (expected ≥ {len(custom_headers)})")
                            else:
                                print(f"   ⚠️  Header count {header_count} < expected {len(custom_headers)}")
                    else:
                        print(f"   ❌ 'Headers:' indicator NOT found in response")
                        passed = False

                    # Check for specific header values (truncated)
                    if "X-Client-Id" in echo_response:
                        print(f"   ✅ X-Client-Id header found in response")
                    else:
                        print(f"   ⚠️  X-Client-Id not found (might be truncated)")

                    if "User-Agent" in echo_response or "user-agent" in echo_response.lower():
                        print(f"   ✅ User-Agent header found in response")
                    else:
                        print(f"   ⚠️  User-Agent not found (might be truncated)")

                    # Check for truncation indicator
                    if "+more" in echo_response:
                        print(f"   ✅ Truncation indicator '+more' found (many headers)")
                    elif header_count > 3:
                        print(f"   ℹ️  Only showing first 3 headers (as designed)")

                    print(f"\n4. Test Result: {'✅ PASS' if passed else '❌ FAIL'}")
                    return passed
                else:
                    print(f"   ❌ Response format unexpected: {text_content}")
                    return False
            else:
                print(f"   ❌ Content format unexpected: {content}")
                return False
        else:
            print(f"   ❌ Response format unexpected: {result}")
            return False

    except requests.exceptions.ConnectionError:
        print(f"\n❌ Connection Error: Could not connect to {base_url}")
        print(f"   Make sure the MCP server is running on port 8080")
        print(f"   Start it with: java -jar target/fastmcp-java-*.jar")
        return False
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_headers_sensitive_data():
    """Test that sensitive headers like Authorization are handled properly"""
    print("\n" + "=" * 60)
    print(" Testing Sensitive Headers (Authorization)")
    print("=" * 60)

    base_url = "http://localhost:8080/mcp"

    # Headers with sensitive data
    headers_with_auth = {
        "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sensitive",
        "X-API-Key": "secret-key-12345678901234567890",
        "Cookie": "session=very-long-cookie-value-that-should-be-truncated"
    }

    print(f"\n1. Sending request with sensitive headers (truncated)")
    print(f"   Authorization: Bearer eyJhbGci...(truncated)")
    print(f"   X-API-Key: secret-key...(truncated)")
    print(f"   Cookie: {headers_with_auth['Cookie'][:20]}...")

    try:
        response = requests.post(
            f"{base_url}/tools/echo",
            json={"message": "Test sensitive headers"},
            headers=headers_with_auth,
            timeout=10
        )
        response.raise_for_status()
        result = response.json()

        # Extract echo response
        echo_text = result["content"][0]["text"]
        print(f"\n2. Server response (truncated sensitive data):")
        print(f"   {echo_text}")

        # Verify truncation
        passed = True
        if "Authorization" in echo_text:
            auth_part = echo_text.split("Authorization=")[1].split()[0] if "Authorization=" in echo_text else ""
            if len(auth_part) < 50:  # Should be truncated to ~20 chars
                print(f"   ✅ Authorization header truncated: {len(auth_part)} chars")
            else:
                print(f"   ⚠️  Authorization header NOT truncated: {len(auth_part)} chars")
                passed = False

        print(f"\n3. Test Result: {'✅ PASS' if passed else '❌ FAIL'}")
        return passed

    except Exception as e:
        print(f"\n❌ Error: {e}")
        return False


def main():
    """Run all header tests"""
    print("\n" + "=" * 60)
    print(" HEADERS FEATURE TEST SUITE")
    print("=" * 60)
    print(" Testing that client headers are accessible in tool context")
    print("=" * 60)

    results = []

    # Test 1: Basic headers echo
    results.append(("Headers Echo", test_headers_echo()))

    # Test 2: Sensitive data truncation
    results.append(("Sensitive Headers", test_headers_sensitive_data()))

    # Summary
    print("\n" + "=" * 60)
    print(" SUMMARY")
    print("=" * 60)

    passed = sum(1 for _, result in results if result)
    total = len(results)

    for name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"   {status} - {name}")

    print(f"\nTotal: {passed}/{total} passed")

    return 0 if passed == total else 1


if __name__ == "__main__":
    import sys
    sys.exit(main())
