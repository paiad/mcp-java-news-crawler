# Tool Contract

## Supported JSON-RPC Methods

- `initialize`
- `initialized`
- `tools/list`
- `tools/call`
- `ping`

Unknown methods must return JSON-RPC error `-32601`.

## Initialize Response

The server returns:

- `protocolVersion`
- `serverInfo.name`
- `serverInfo.version`
- `capabilities.tools`
- `instructions`

Current protocol version is `2024-11-05`.

## tools/list Response

Each tool entry includes:

- `name`
- `description`
- `inputSchema`

## tools/call Response

Successful and failed tool invocations both return JSON-RPC `result`.

Result shape:

```json
{
  "content": [
    {
      "type": "text",
      "text": "..."
    }
  ],
  "isError": false
}
```

## Error Semantics

- Unknown tool name returns `result.isError = true`
- Tool exception returns `result.isError = true`
- Parse failure returns JSON-RPC error `-32700`
- Unknown method returns JSON-RPC error `-32601`
- `initialized` is a notification and returns no payload

## Current Tools

- `get_hot_news`
- `search_news`

When changing tool inputs or outputs, update tests before changing behavior.
