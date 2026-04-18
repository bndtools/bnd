---
title: JAR Lifecycle Listener
layout: bnd
parent: Plugins
nav_order: 10
---

# JAR Lifecycle Listener Plugin

The **`JarLifecycleListener`** interface allows plugins to observe and participate in the JAR artifact lifecycle without modifying core bnd code.

## Purpose

This plugin enables:

- **Metadata generation** – Compute and store digests, checksums, signatures
- **Rebuild optimization** – Detect unchanged content/API to prevent cascade rebuilds
- **JAR signing** – Sign artifacts post-write
- **Artifact tracking** – Index or report built artifacts
- **Binary validation** – Post-write scanning or verification
- **Bytecode enhancement** – Transform JAR contents before write

## Interface

```java
public interface JarLifecycleListener {

    /**
     * Called before the JAR is written to disk.
     * Store computed metadata in context for the afterWrite phase.
     */
    default void beforeWrite(Project project, Jar jar, File outputFile, Map<String, Object> context)
        throws Exception {}

    /**
     * Called after the JAR is written to disk.
     * Retrieve metadata from context and perform post-write actions.
     */
    default void afterWrite(Project project, File outputFile, Jar jar, Map<String, Object> context)
        throws Exception {}

}
```

## Phases

### beforeWrite

Called **before** the JAR is written to disk.

**Use cases:**
- Compute metadata (digests, checksums, API signatures) based on in-memory JAR state
- Validate the JAR before persisting
- Prepare data structures for the after phase

**Context:** The `context` map is empty. Store computed values for use in `afterWrite`.

### afterWrite

Called **after** the JAR is successfully written to disk.

**Use cases:**
- Store metadata in sidecar files
- Modify file properties (timestamps, permissions)
- Trigger post-write notifications
- Validate the written file

**Context:** The `context` map contains data stored by `beforeWrite`.

## Context Map

The `context` parameter enables stateless, thread-safe data exchange between phases:

```java
public class MyListener implements JarLifecycleListener {

    @Override
    public void beforeWrite(Project project, Jar jar, File outputFile, Map<String, Object> context) {
        // Compute metadata
        byte[] digest = jar.getTimelessDigest();
        String digestHex = Hex.toHexString(digest);

        // Store in context for after phase
        context.put("contentDigest", digestHex);
    }

    @Override
    public void afterWrite(Project project, File outputFile, Jar jar, Map<String, Object> context) {
        // Retrieve metadata computed in before phase
        String digestHex = (String) context.get("contentDigest");

        // Perform post-write action
        File digestFile = new File(outputFile.getParentFile(),
                                  outputFile.getName() + ".digest");
        IO.store(digestHex, digestFile);
    }
}
```

**Key properties:**
- ✅ **Thread-safe** – Each JAR write gets a fresh map instance
- ✅ **Stateless listeners** – Listeners don't store state; data is externalized
- ✅ **Composable** – Multiple listeners can share context keys
- ✅ **Testable** – Easy to mock for unit tests


## Error Handling

If a listener throws an exception:
- The exception is **logged at debug level** (not as an error)
- **Other listeners continue** to run
- **The JAR write is unaffected** – exceptions do not roll back the write

Avoid throwing exceptions for informational messages; use logging instead.

## Registration

### In Code

```java
JarLifecycleListener listener = new MyListener();
workspace.addBasicPlugin(listener);
```

### In Configuration

Add to `cnf/build.bnd` or a file in `cnf/ext/`:

```properties
-plugin.my-listener = com.example.MyJarListener
```

## Examples

### Example 1: Simple Digest Sidecar

```java
public class DigestListener implements JarLifecycleListener {

    @Override
    public void beforeWrite(Project project, Jar jar, File outputFile, Map<String, Object> context)
            throws Exception {
        byte[] digest = jar.getTimelessDigest();
        context.put("digest", Hex.toHexString(digest));
    }

    @Override
    public void afterWrite(Project project, File outputFile, Jar jar, Map<String, Object> context)
            throws Exception {
        String digestHex = (String) context.get("digest");
        File digestFile = new File(outputFile.getParentFile(),
                                  outputFile.getName() + ".digest");
        IO.store(digestHex, digestFile);
    }
}
```

## Best Practices

1. **Compute expensive metadata in `beforeWrite`** – Before the JAR is written
2. **Use context for data exchange** – Don't store state in the listener
3. **Log at debug level** – Use `logger.debug()` for diagnostic messages
4. **Avoid throwing exceptions** – Use logging for non-fatal issues
5. **Make listeners stateless** – Thread-safe plugins work with parallel builds

## See Also

- [Plugin Overview](00-overview.html)
- [Bndtools Rebuild Policy Plugin](bndtools-rebuild-policy-plugin.html) – Example bndtools implementation

