package org.bndtools.builder;

import java.util.Formatter;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IncrementalProjectBuilder;

public class BuildLogger {
	public static final int		LOG_FULL	= 2;
	public static final int		LOG_BASIC	= 1;
	public static final int		LOG_NONE	= 0;
	private static final org.slf4j.Logger	perfLogger	= org.slf4j.LoggerFactory.getLogger("bndtools.builder.perf");
	private final int			level;
	private final String		name;
	private final int			kind;
	private final long			startNanos	= System.nanoTime();
	private final StringBuilder	sb			= new StringBuilder();
	private final Formatter		formatter	= new Formatter(sb);
	private boolean				used		= false;
	private int					files		= -1;

	public BuildLogger(int level, String name, int kind) {
		this.level = level;
		this.name = name;
		this.kind = kind;
	}

	public void basic(String string) {
		basic(string, (Object[]) null);
	}

	public void basic(String string, Object... args) {
		if (level < LOG_BASIC)
			return;

		message(string, args);
	}

	public void full(String string) {
		full(string, (Object[]) null);
	}

	public void full(String string, Object... args) {
		if (level < LOG_FULL)
			return;

		message(string, args);
	}

	public boolean isEmpty() {
		return !used;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	private void message(String string, Object[] args) {
		used = true;
		if (args == null) {
			sb.append(string);
		} else {
			formatter.format(string, args);
		}
		sb.append('\n');
	}

	public String format() {
		long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

		StringBuilder top = new StringBuilder().append("BUILD ");
		try (Formatter topper = new Formatter(top)) {
			String kindStr;
			switch (kind) {
				case IncrementalProjectBuilder.FULL_BUILD :
					kindStr = "FULL";
					break;
				case IncrementalProjectBuilder.AUTO_BUILD :
					kindStr = "AUTO";
					break;
				case IncrementalProjectBuilder.CLEAN_BUILD :
					kindStr = "CLEAN";
					break;
				case IncrementalProjectBuilder.INCREMENTAL_BUILD :
					kindStr = "INCREMENTAL";
					break;
				default :
					kindStr = Integer.toString(kind);
					break;
			}
			top.append(kindStr)
				.append(' ')
				.append(name)
				.append(' ');
			if (files == 1) {
				top.append("1 file was built");
			} else if (files > 1) {
				topper.format("%d files were built", files);
			} else {
				top.append("no build");
			}

			long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
			long millis = duration % TimeUnit.SECONDS.toMillis(1L);
			topper.format(" in %d.%03d sec", seconds, millis);

			// Always log performance data for builds
			perfLogger.debug("{} {} {} files={} duration={}ms", kindStr, name,
				files > 0 ? "built" : "skipped", files, duration);
		}

		return top.append('\n')
			.append(sb)
			.toString();
	}

	public boolean isActive() {
		return level != LOG_NONE;
	}

	public void setFiles(int f) {
		files = f;
	}
}
