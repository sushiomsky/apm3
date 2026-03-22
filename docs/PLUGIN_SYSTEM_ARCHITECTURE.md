# Plugin System Architecture

## Overview

The plugin system enables extensible RFID workflows through a secure, sandboxed architecture that supports multiple plugin types while maintaining app stability and security.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Plugin Host Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │
│  │ Plugin Store │  │ Plugin       │  │ Permission         │   │
│  │ & Registry   │  │ Lifecycle    │  │ Manager            │   │
│  └──────────────┘  └──────────────┘  └────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────┼─────────────────────────────────┐
│                    Plugin API Contract                         │
│  ┌───────────────────────────────────────────────────────────┐│
│  │  PluginInterface                                          ││
│  │  - onLoad(), onUnload()                                   ││
│  │  - execute(context, params): PluginResult                ││
│  │  - getMetadata(): PluginMetadata                          ││
│  └───────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────┴─────────────────────────────────┐
│                      Plugin Types                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐ │
│  │ Attack   │  │ Protocol │  │Automation│  │ Data         │ │
│  │ Modules  │  │ Handlers │  │ Scripts  │  │ Processors   │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────┴─────────────────────────────────┐
│                    Core App Services                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐ │
│  │ Proxmark │  │ Terminal │  │   Tag    │  │   Feature    │ │
│  │ Client   │  │   I/O    │  │  Store   │  │  Execution   │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

### 1. Plugin Host Layer
- **PluginRegistry**: Central registry for all installed plugins
- **PluginLoader**: Loads plugin classes and validates signatures
- **PluginSandbox**: Isolates plugin execution with restricted capabilities
- **PermissionManager**: Enforces permission model for plugin operations

### 2. Plugin API Contract
Core interfaces that all plugins implement:
- `PluginInterface`: Base plugin lifecycle and execution
- `PluginMetadata`: Descriptive information (name, author, version, etc.)
- `PluginContext`: Restricted access to app services
- `PluginResult`: Standardized output format

### 3. Plugin Types

#### Attack Modules
Implement new attack algorithms (e.g., custom Mifare attacks, timing analysis)
- Interface: `AttackPlugin`
- Permissions: `SEND_COMMANDS`, `READ_TAGS`, `ANALYZE_DATA`

#### Protocol Handlers
Add support for custom RFID protocols
- Interface: `ProtocolPlugin`
- Permissions: `SEND_COMMANDS`, `PARSE_OUTPUT`, `READ_TAGS`

#### Automation Scripts
Batch operations and workflow automation
- Interface: `AutomationPlugin`
- Permissions: `EXECUTE_FEATURES`, `READ_TAGS`, `WRITE_TAGS`

#### Data Processors
Custom parsers and data transformation
- Interface: `DataProcessorPlugin`
- Permissions: `READ_DATA`, `TRANSFORM_DATA`

#### UI Extensions
New visualization and display types
- Interface: `UiPlugin`
- Permissions: `RENDER_UI`, `READ_DATA`

## Security Model

### Permission Levels
1. **MINIMAL** (read-only data access)
2. **STANDARD** (execute safe commands)
3. **ELEVATED** (write operations, advanced commands)
4. **FULL** (system-level access - requires user approval)

### Sandboxing Strategy
- ClassLoader isolation per plugin
- Restricted API surface via PluginContext
- Timeout enforcement (30s default, configurable)
- Memory limits per plugin (64MB default)
- No direct filesystem access
- No network access without explicit permission

### Security Checks
1. **Installation**: Verify plugin signature and manifest
2. **Load Time**: Validate permissions match declared manifest
3. **Runtime**: Check permission before each sensitive operation
4. **Update**: Re-verify signature and permissions

## Data Flow

### Plugin Execution Flow
```
User Action
    ↓
PluginManager.executePlugin(id, params)
    ↓
PermissionManager.checkPermissions(plugin, operation)
    ↓
PluginSandbox.execute(plugin, context, params)
    ↓
Plugin.execute(context, params)
    ↓
Return PluginResult(success, output, logs)
    ↓
Update UI with results
```

### Event System
Plugins can subscribe to app events:
- `onTagScanned(TagInfo)`
- `onConnectionStateChanged(ReaderState)`
- `onFeatureExecuted(FeatureResult)`
- `onTerminalOutput(String)`

Publish custom events:
- `publishEvent(type, data)` - Other plugins can subscribe

## Workflow Builder

### Workflow Model
```kotlin
data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<WorkflowStep>,
    val variables: Map<String, Any>
)

sealed class WorkflowStep {
    data class ExecutePlugin(val pluginId: String, val params: Map<String, Any>)
    data class Condition(val expression: String, val thenStep: WorkflowStep, val elseStep: WorkflowStep?)
    data class Loop(val iterations: Int, val body: WorkflowStep)
    data class Variable(val name: String, val value: Any)
}
```

### Execution Engine
- Sequential step execution
- Variable passing between steps
- Conditional branching with expression evaluation
- Error handling and rollback
- Debug mode with breakpoints

## Plugin Distribution

### Plugin Package Format
```
plugin.apkp (Android Plugin Package)
├── manifest.json          # Metadata and permissions
├── plugin.jar            # Compiled plugin code
├── resources/            # Icons, strings, etc.
├── signature.sig         # Digital signature
└── README.md             # Documentation
```

### Manifest Schema
```json
{
  "id": "com.example.mifare.custom_attack",
  "name": "Custom Mifare Attack",
  "version": "1.0.0",
  "author": "John Doe",
  "description": "Advanced nested authentication attack",
  "type": "attack",
  "permissions": [
    "SEND_COMMANDS",
    "READ_TAGS",
    "ANALYZE_DATA"
  ],
  "minAppVersion": "1.0.0",
  "dependencies": [],
  "entry_point": "com.example.mifare.CustomAttackPlugin"
}
```

## Storage

### Plugin Registry Database
```sql
CREATE TABLE plugins (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    author TEXT,
    type TEXT NOT NULL,
    enabled INTEGER DEFAULT 1,
    installed_at INTEGER,
    last_updated INTEGER,
    permissions TEXT, -- JSON array
    settings TEXT     -- JSON object
);

CREATE TABLE plugin_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plugin_id TEXT NOT NULL,
    started_at INTEGER,
    completed_at INTEGER,
    success INTEGER,
    output TEXT,
    FOREIGN KEY(plugin_id) REFERENCES plugins(id)
);

CREATE TABLE workflows (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    steps TEXT NOT NULL, -- JSON array
    variables TEXT,      -- JSON object
    created_at INTEGER
);
```

## Performance Considerations

1. **Lazy Loading**: Load plugin classes only when needed
2. **Caching**: Cache plugin metadata and compiled code
3. **Background Execution**: Run plugins on IO dispatcher
4. **Resource Limits**: Enforce memory and CPU limits
5. **Preemption**: Allow cancellation of long-running plugins

## Future Extensions

1. **Remote Plugin Store**: Fetch plugins from cloud repository
2. **Community Ratings**: User reviews and ratings
3. **Automatic Updates**: Background plugin updates
4. **Plugin Marketplace**: Paid plugins with licensing
5. **Cross-Platform**: Share plugins between Android and desktop
6. **Visual Programming**: Block-based workflow editor
7. **AI Integration**: AI-assisted workflow creation
