package aQute.bnd.build;

import static aQute.libg.slf4j.GradleLogging.LIFECYCLE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.progress.ProgressPlugin;

public class LoggingProgressPlugin implements ProgressPlugin {

	private final static Logger logger = LoggerFactory.getLogger(LoggingProgressPlugin.class);

	@Override
	public Task startTask(final String name, final int size) {
		logger.info(LIFECYCLE, name); // log at Gradle LIFECYCLE level

		return new Task() {
			@Override
			public void done(String message, Throwable e) {
				if (e != null) {
					logger.error(message, e);
				} else {
					logger.debug(message);
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
