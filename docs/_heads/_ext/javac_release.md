`javac.release` configures the `--release` option of `javac`. For example, `javac.release: 8` compiles with the Java 8 language and class-file rules and exposes only the documented Java 8 platform API, even when the compiler runs on a newer JDK.

`javac.release` takes precedence over `javac.source` and `javac.target`. Do not configure `javac.profile`, a bootclasspath, or conflicting `--source`/`--target` options with it; those options are not valid together with `--release`.

Set `javac.release:` to an empty value to opt out and use the legacy `-source` and `-target` options. Legacy mode does not restrict access to newer platform APIs unless an appropriate bootclasspath is supplied.

When `javac.release` is not set, bnd automatically uses `--release` when `javac.source` and `javac.target` denote the same release, the compiler supports `--release`, and no profile or bootclasspath is configured.

## Compiler scenarios

The compiler JDK is the JDK that supplies the configured `javac` executable. It may differ from the JDK that runs bnd.

| bnd execution JDK | Compiler JDK | `javac.source` | `javac.target` | Result |
| --- | --- | --- | --- | --- |
| 17 | 17 | `1.8` | `1.8` | Automatic `--release 8`; Java 8 language, class-file, and platform API rules. |
| 17 | 21 | `1.8` | `1.8` | Automatic `--release 8`; newer JDK APIs are unavailable. |
| 17 | 25 | `1.8` | `1.8` | Automatic `--release 8`; newer JDK APIs are unavailable. |
| 17 | 17 | `17` | `17` | Automatic `--release 17`. |
| 17 | 21 | `11` | `8` | Legacy `-source 11 -target 8`; no automatic `--release` because source and target differ. |
| 17 | 8 | `1.8` | `1.8` | Legacy `-source 1.8 -target 1.8`; JDK 8 does not support `--release`. |

The bnd runtime requires JDK 17 or newer. The compiler JDK can be older if its `javac` executable is configured with the `javac` property, for example:

```properties
javac: /path/to/jdk-8/bin/javac
javac.source: 1.8
javac.target: 1.8
```

This does not lower bnd's runtime requirement. An older compiler must support the requested source and target levels, and it cannot use `--release` when the compiler itself predates JDK 9. Conversely, configuring `javac.release: 8` with a JDK 8 compiler is invalid because that compiler does not recognize `--release`; use legacy mode instead:

```properties
javac: /path/to/jdk-8/bin/javac
javac.release:
javac.source: 1.8
javac.target: 1.8
```
