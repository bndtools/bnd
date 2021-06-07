package aQute.bnd.service;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.bnd.build.Project;
import aQute.service.reporter.Reporter;

public class BndListener {
	final AtomicInteger inside = new AtomicInteger();

	public void changed(File file) {}

	public void built(Project project, Collection<File> files) {}

	public void begin() {
		inside.incrementAndGet();
	}

	public void end() {
		inside.decrementAndGet();
	}

	public boolean isInside() {
		return inside.get() != 0;
	}

	public void signal(Reporter reporter) {}
}
