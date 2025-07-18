---
layout: default
class: Project
title: -runjdb  ADDRESS
summary: Specify a JDB socket transport address on invocation when launched outside a debugger so the debugger can attach later. 
---

# -runjdb

The `-runjdb` instruction specifies a JDB (Java Debugger) socket transport address to use when launching the application outside a debugger. This allows the debugger to attach to the running process for debugging purposes.

Example:

```
-runjdb: localhost:10001
```

The address can include a host name or IP address and a port. This is useful for remote debugging scenarios.

This instruction launches the VM with the

    -agentlib:jdwp=transport=dt_socket,server=y,address=<address>,suspend=y
 
 command line argument.

The socket transport address can include a host name (or IP address) and a port. For example:

    -runjdb: localhost:10001

The socket transport address can be just a port number. For example:

    -runjdb: 10001

Note: Starting with Java 9, using just a port number means that the launched VM will only listen on `localhost` for the connection. If you want to remote debug, you will need to specify a host name (or IP address), or an asterisk (`*`) to accept from any host. For example:

    -runjdb: *:10001

If the socket transport address starts with a minus sign (`-`), then the launched VM is not suspended: `suspend=n`. The minus sign is removed from the socket transport address before it is used for the `address` option.

If the specified socket transport address is not a port number or a host:port value, then the address `1044` is used.


---
TODO Needs review - AI Generated content