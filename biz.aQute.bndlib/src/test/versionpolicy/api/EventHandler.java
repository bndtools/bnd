package test.versionpolicy.api;

import aQute.bnd.annotation.*;

@SuppressWarnings("deprecation") @UsePolicy
@ConsumerType
public interface EventHandler {
    void listen(Object o);
}
