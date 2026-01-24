package com.ultrathink.fastmcp.schema;

import java.util.Map;

public class JsonSchemaImpl implements JsonSchema {
    private final Map<String, Object> map;

    public JsonSchemaImpl(Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public Map<String, Object> asMap() {
        return map;
    }
}
