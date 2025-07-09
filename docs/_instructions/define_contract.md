---
layout: default
title: -define-contract
class: Project
summary: |
   Define a contract when one cannot be added to the buildpath.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-define-contract:\
  osgi.contract;\
    osgi.contract=JavaServlet;\
    uses:="javax.servlet,javax.servlet.annotation,javax.servlet.descriptor,javax.servlet.http";\
    version:Version="3.0"`

- Pattern: `.*`

<!-- Manual content from: ext/define_contract.md --><br /><br />

Used in conjunction with the [`-contract`](contract.html) instruction,`-define-contract` can be applied in order to define a contract which is not available on the build path (i.e. compile class path).

The contract specification is exactly the same syntax used in the `Provide-Capability` header.

```properties
-define-contract:\
  osgi.contract;\
    osgi.contract=JavaServlet;\
    uses:="javax.servlet,javax.servlet.annotation,javax.servlet.descriptor,javax.servlet.http";\
    version:Version="3.0"
```

Now if the current bundle imports packages declared in the `uses` directive of the defined contract above, they will be imported without a package version, and a contract requirement will be added exactly as if there had been a contract on the build path.

**Note** the `-contract` instruction defaults to `*` (all contracts) and so it can be omitted when using the `-define-contract` instruction.

### Further Reading

- See [-contract](/instructions/contract.html) and [-define-contract](/instructions/define_contract.html) instruction for defining contracts in bnd.
- See also [Portable Contract Definitions](https://docs.osgi.org/reference/portable-java-contracts.html)
