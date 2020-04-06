---
layout: default
class: Project
title: -define-contract 
summary: Define a contract when one cannot be added to the buildpath. 
---

Used in conjunction with the [`-contract`](contract.html) instruction,`-define-contract` can be applied in order to define a contract which is not available on the build path (i.e. compile class path).

The contract specification is exactly the same syntax used in the `Provide-Capability` header.

```properties
-define-contract:\
  osgi.contract;\
    osgi.contract=Servlet;\
    uses:="javax.servlet,javax.servlet.http";\
    version="3.0"
```

Now if the current bundle imports packages declared in the `uses` directive of the defined contract above, they will be imported without a package version, and a contract requirement will be added exactly as if there had been a contract on the build path.

**Note** the `-contract` instruction defaults to `*` (all contracts) and so it can be omitted when using the `-define-contract` instruction.

### Further Reading

See also [Portable Contract Definitions](https://www.osgi.org/portable-java-contract-definitions/)


