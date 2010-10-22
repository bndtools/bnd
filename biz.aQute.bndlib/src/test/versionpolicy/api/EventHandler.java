package test.versionpolicy.api;

import aQute.bnd.annotation.*;

@UsePolicy
@ConsumerType
public interface EventHandler {
    void listen(Object o);
}
