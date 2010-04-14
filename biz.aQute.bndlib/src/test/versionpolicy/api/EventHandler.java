package test.versionpolicy.api;

import aQute.bnd.annotation.*;

@UsePolicy
public interface EventHandler {
    void listen(Object o);
}
