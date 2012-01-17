package test.versionpolicy.api;

import aQute.bnd.annotation.*;

@UsePolicy
@ConsumerType
@SuppressWarnings("deprecation") public interface EventHandler {
    void listen(Object o);
}
