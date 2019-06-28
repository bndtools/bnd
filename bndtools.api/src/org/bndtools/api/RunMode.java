package org.bndtools.api;

import java.util.Optional;

import aQute.bnd.build.Run;

public enum RunMode {
	/*
	 * Mode used when editing the run
	 */
	EDIT,
	/*
	 * Mode used when exporting the run
	 */
	EXPORT,
	/*
	 * Mode used when launching the run
	 */
	LAUNCH,
	/*
	 * This mode when locating source containers from the run
	 */
	SOURCES,
	/*
	 * Mode used when launching the run for testing
	 */
	TEST,
	/*
	 * Mode was not set on the run
	 */
	UNSET;

	public static RunMode get(Run run) {
		return Optional.ofNullable(run.get(RunMode.class.getName()))
			.map(RunMode::valueOf)
			.orElse(UNSET);
	}

	public static void set(Run run, RunMode mode) {
		run.set(RunMode.class.getName(), mode.name());
	}

}
