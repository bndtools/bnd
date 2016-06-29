package aQute.bnd.build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import aQute.bnd.service.progress.ProgressPlugin;

public class LoggingProgressPlugin implements ProgressPlugin {

	private final static Logger	logger	= LoggerFactory.getLogger(LoggingProgressPlugin.class);
	/**
	 * If we are running under Gradle, get the LIFECYCLE marker to use when
	 * logging.
	 */
	private final static Marker	LIFECYCLE;
	static {
		Marker lifecycle = null;
		try {
			Class< ? > logging = Class.forName("org.gradle.api.logging.Logging");
			lifecycle = (Marker) logging.getField("LIFECYCLE").get(null);
		} catch (Exception e) {}
		LIFECYCLE = lifecycle;
	}

	@Override
	public Task startTask(final String name, final int size) {
		if (LIFECYCLE != null) {
			logger.info(LIFECYCLE, name); // log at Gradle LIFECYCLE level
		} else {
			logger.info(name);
		}

		return new Task() {
			@Override
			public void done(String message, Throwable e) {
				if (e != null) {
					logger.error(message, e);
				}
			}

			@Override
			public boolean isCanceled() {
				return false;
			}

			@Override
			public void worked(int units) {}
		};
	}
}
