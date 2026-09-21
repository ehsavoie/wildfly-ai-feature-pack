---
title: WASM Support
description: WebAssembly WASI module integration -- run sandboxed Wasm tools as MCP tools in WildFly.
layout: page
---

# WASM Support (Proof of Concept)

The feature pack includes a proof-of-concept integration for [WASI](https://wasi.dev/) (WebAssembly System Interface) modules using the [Chicory](https://github.com/nicktomlin/chicory) Java WASM runtime and the [Extism](https://extism.org/) SDK.

## Setup

Include the `wasm` Galleon layer when provisioning your WildFly server:

```xml
<layers>
    <layer>cloud-server</layer>
    <layer>wasm</layer>
</layers>
```

## API

Add the WASM API as a provided dependency:

```xml
<dependency>
    <groupId>org.wildfly</groupId>
    <artifactId>wildfly-wasm-api</artifactId>
    <scope>provided</scope>
</dependency>
```

### WasmInvoker

WASM binaries defined in the `wasm` subsystem can be injected as `org.wildfly.wasm.api.WasmInvoker` via CDI:

```java
@Inject
WasmInvoker wasmInvoker;
```

### WasmToolService

You can expose `org.wildfly.wasm.api.WasmToolService` instances as MCP tools, bridging WASM execution with the MCP protocol.

## Security

WASM modules execute in a sandboxed environment with configurable constraints:

- **Memory limits** -- Set maximum memory for module execution
- **Network access** -- Control whether modules can make network calls
- **Module paths** -- Admin-configured paths restrict which WASM binaries can be loaded

## Example

See the [wildfly-weather WASM branch](https://github.com/ehsavoie/wildfly-weather/compare/wasm_subsystem) for a working example.

## Current Versions

| Component | Version |
|-----------|---------|
| Chicory | 1.7.5 |
| Extism SDK | 0.3.0 |
