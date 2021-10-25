package aQute.remote.agent;

import static java.util.Objects.requireNonNull;

import org.osgi.service.log.LogReaderService;

public class XLogReaderAdmin {

    private XLogReaderAdmin() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

	public static void register(Object service, OSGiLogListener logListener) {
        requireNonNull(service);
        requireNonNull(logListener);
        if (service instanceof LogReaderService) {
            ((LogReaderService) service).addLogListener(logListener);
        }
    }

	public static void unregister(Object service, OSGiLogListener logListener) {
        requireNonNull(service);
        requireNonNull(logListener);
        if (service instanceof LogReaderService) {
            ((LogReaderService) service).removeLogListener(logListener);
        }
    }

}
