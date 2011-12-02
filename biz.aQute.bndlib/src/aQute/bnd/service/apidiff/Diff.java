package aQute.bnd.service.apidiff;

import java.util.*;

import aQute.libg.version.*;

public interface Diff {
	Delta getDelta();

	Type getType();

	Version getOlderVersion();

	Version getNewerVersion();

	Collection<Diff> getChildren();
}
