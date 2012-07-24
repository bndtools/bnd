package aQute.bnd.service;

import java.io.*;
import java.util.concurrent.atomic.*;

import aQute.service.reporter.*;

public class BndListener {
	final AtomicInteger	inside	= new AtomicInteger();

	public void changed(@SuppressWarnings("unused") File file) {}

	public void begin() {
		inside.incrementAndGet();
	}

	public void end() {
		inside.decrementAndGet();
	}

	public boolean isInside() {
		return inside.get() != 0;
	}

	public void signal(@SuppressWarnings("unused") Reporter reporter) {

	}
}
