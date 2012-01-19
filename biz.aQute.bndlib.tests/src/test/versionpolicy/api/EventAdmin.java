package test.versionpolicy.api;

import aQute.bnd.annotation.*;

@ProviderType
public interface EventAdmin {
    void post(Object o);
}
