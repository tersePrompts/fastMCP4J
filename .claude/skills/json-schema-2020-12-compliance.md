# JSON Schema 2020-12 Compliance for MCP Tools

> **Skill**: Ensure MCP tool schemas comply with JSON Schema 2020-12 specification

---

## Problem

Claude's API validates tool input schemas against JSON Schema 2020-12. Invalid schemas cause:
```
API Error: 400 {"type":"error","error":{"type":"invalid_request_error",
"message":"tools.X.input_schema: JSON schema is invalid. It must match JSON Schema draft 2020-12"}}
```

---

## Solution

### 1. Add `$schema` Declaration

Every schema MUST declare the JSON Schema version:

```java
public Map<String, Object> generate(Method method) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    schema.put("type", "object");
    // ... rest of schema generation
}
```

### 2. Remove Non-Standard Keywords

These keywords are **NOT** valid in JSON Schema 2020-12:

| Keyword | Status | Alternative |
|---------|--------|-------------|
| `examples` | ❌ Removed | Put in `description` |
| `constraints` | ❌ Not valid | Put in `description` |
| `hints` | ❌ Not valid | Put in `description` |
| `required` at property level | ❌ Not valid | Use at root only |

### 3. Embed Metadata in Description

```java
// ❌ WRONG - non-standard fields
schema.put("examples", Arrays.asList("value1", "value2"));
schema.put("constraints", "Must be valid email");
schema.put("hints", "Use for user login");

// ✅ CORRECT - embed in description
StringBuilder description = new StringBuilder("The user's email address.");
description.append("\n\nExamples: value1, value2");
description.append("\n\nConstraints: Must be valid email");
description.append("\n\nHints: Use for user login");
schema.put("description", description.toString());
```

### 4. `required` at Root Level Only

```java
// ✅ CORRECT - required at root
schema.put("required", List.of("email", "name"));

// ❌ WRONG - required at property level
paramSchema.put("required", false);  // This is invalid!
```

---

## Valid JSON Schema 2020-12 Keywords

### Core Keywords
- `$schema` - Meta-schema identifier
- `$id` - Schema identifier
- `$ref` - Reference
- `type` - Data type (string, number, object, array, boolean, null)

### Object Keywords
- `properties` - Property definitions
- `required` - Required properties (array of strings, at root only)
- `additionalProperties` - Additional properties rule
- `patternProperties` - Pattern-based properties
- `minProperties` / `maxProperties` - Property count limits

### Array Keywords
- `items` - Array item schema
- `additionalItems` - Additional items rule
- `minItems` / `maxItems` - Array length limits
- `uniqueItems` - Uniqueness requirement
- `contains` - Contains schema

### String Keywords
- `minLength` / `maxLength` - Length limits
- `pattern` - Regex pattern
- `format` - Format modifier (email, uri, date-time, etc.)

### Numeric Keywords
- `minimum` / `maximum` - Value bounds
- `exclusiveMinimum` / `exclusiveMaximum` - Exclusive bounds
- `multipleOf` - Multiple requirement

### General Keywords
- `description` - Human-readable description (widely supported extension)
- `default` - Default value
- `enum` - Fixed enumeration
- `const` - Constant value
- `allOf` / `anyOf` / `oneOf` - Combinations
- `not` - Negation

---

## Example: Correct Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "email": {
      "type": "string",
      "description": "User's email address.\n\nExamples: user@example.com, admin@company.org\n\nConstraints: Must be valid email format\n\nHints: Used for login and notifications"
    },
    "age": {
      "type": "integer",
      "description": "User's age.\n\nConstraints: Must be 18-120\n\nDefaults: Uses account creation date if not provided",
      "minimum": 18,
      "maximum": 120,
      "default": "18"
    }
  },
  "required": ["email"]
}
```

---

## Common Mistakes

### ❌ Adding `examples` as a field
```java
schema.put("examples", Arrays.asList("a", "b"));  // NOT VALID
```

### ❌ Adding `constraints` as a field
```java
schema.put("constraints", "Must be positive");  // NOT VALID
```

### ❌ Adding `hints` as a field
```java
schema.put("hints", "Use lowercase only");  // NOT VALID
```

### ❌ Adding `required` at property level
```java
paramSchema.put("required", false);  // NOT VALID
```

---

## Security Considerations

When updating schemas for compliance:
1. Keep all information - just move it to `description`
2. LLMs can still read examples, constraints, hints from description
3. Don't remove validation - move to schema keywords where possible
4. Use `minimum`, `maximum`, `pattern`, `enum` for actual validation

---

## Testing

Test your schemas with:
1. Claude API - Call a tool and check for validation errors
2. JSON Schema validators - Use online tools to validate
3. Unit tests - Assert schema structure matches expected

---

## References

- [JSON Schema 2020-12 Specification](https://json-schema.org/draft/2020-12/json-schema-core.html)
- [Claude Tool Use Documentation](https://docs.claude.com/en/docs/tool-use)
- [MCP Specification](https://modelcontextprotocol.io/)

---

*Updated: 2026-01-28*
