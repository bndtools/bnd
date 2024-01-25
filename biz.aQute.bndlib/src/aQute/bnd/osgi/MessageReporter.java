package aQute.bnd.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.service.reporter.Report.Location;
import aQute.service.reporter.Reporter;
import aQute.service.reporter.Reporter.SetLocation;

/**
 * This is the error and warning messages collected. It implements a concurrent
 * message handling system.
 */
public class MessageReporter {

	final Processor								processor;
	final AtomicInteger							counter		= new AtomicInteger(1000);
	volatile ConcurrentHashMap<String, Message>	messages	= new ConcurrentHashMap<>();

	class Message extends Location implements SetLocation, Comparable<Message> {
		@Override
		public String toString() {
			return "Message [error=" + error + ", sequence=" + sequence + ", message=" + message + "]";
		}

		final boolean	error;
		final int		sequence;

		Message(int sequence, String message, boolean error) {
			this.sequence = sequence;
			this.message = message;
			this.error = error;
		}

		Message(int sequence, Message m, String actualPrefix) {
			m.to(this);
			this.sequence = sequence;
			this.message = actualPrefix.concat(m.message);
			this.error = m.error;
		}

		@Override
		public SetLocation file(String file) {
			this.file = file;
			return this;
		}

		@Override
		public SetLocation header(String header) {
			this.header = header;
			return this;
		}

		@Override
		public SetLocation context(String context) {
			this.context = context;
			return this;
		}

		@Override
		public SetLocation method(String methodName) {
			this.methodName = methodName;
			return this;
		}

		@Override
		public SetLocation line(int n) {
			this.line = n;
			return this;
		}

		@Override
		public SetLocation reference(String reference) {
			this.reference = reference;
			return this;
		}

		@Override
		public SetLocation details(Object details) {
			this.details = details;
			return null;
		}

		@Override
		public Location location() {
			return this;
		}

		@Override
		public SetLocation length(int length) {
			this.length = length;
			return this;
		}

		@Override
		public int compareTo(Message o) {
			return Integer.compare(sequence, o.sequence);
		}

		void fixup(Instructions fixupInstrs, List<String> errors, List<String> warnings) {

			boolean error = this.error;

			Instruction matcher = fixupInstrs.finder(message);
			String type = error ? Constants.FIXUPMESSAGES_IS_ERROR : Constants.FIXUPMESSAGES_IS_WARNING;
			String message = this.message;

			if (matcher != null && !matcher.isNegated()) {

				Attrs attrs = fixupInstrs.get(matcher);
				String restrict = attrs.get(Constants.FIXUPMESSAGES_RESTRICT_DIRECTIVE);
				String replace = attrs.get(Constants.FIXUPMESSAGES_REPLACE_DIRECTIVE);
				String is = attrs.get(Constants.FIXUPMESSAGES_IS_DIRECTIVE);

				if (restrict == null || restrict.equals(type)) {

					if (is != null && !is.equals(type)) {
						error = !error;
					}

					if (replace != null) {
						try (Processor replacer = new Processor(processor())) {
							replacer.setProperty("@", message);
							processor().getLogger()
								.debug("replacing {} with {}", message, replace);
							message = replacer.getReplacer()
								.process(replace);
						} catch (Exception e) {
							throw Exceptions.duck(e);
						}
					}

					if (attrs.isEmpty() || Constants.FIXUPMESSAGES_IS_IGNORE.equals(is)) {
						message = null;
					}
				}
			}
			if (message != null) {
				if (error)
					errors.add(message);
				else
					warnings.add(message);
			}
		}

	}

	class Cache {
		final List<String>	errors		= new ArrayList<>();
		final List<String>	warnings	= new ArrayList<>();

		Cache() {
			boolean failOk = processor().isFailOk();
			Instructions fixupInstrs = new Instructions();
			Parameters fixup = processor().getMergedParameters(Constants.FIXUPMESSAGES);
			fixup.forEach((k, v) -> fixupInstrs.put(Instruction.legacy(k), v));

			messages.values()
				.stream()
				.sorted()
				.forEach(m -> {
					m.fixup(fixupInstrs, failOk ? warnings : errors, warnings);
				});
		}
	}

	MessageReporter(Processor processor) {
		this.processor = processor;
	}

	List<String> getWarnings() {
		return fixup().warnings;
	}

	List<String> getErrors() {
		return fixup().errors;
	}

	Cache fixup() {
		return new Cache();
	}

	Location getLocation(String msg) {
		return messages.get(msg);
	}

	public SetLocation error(String format, Object... args) {
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Throwable t) {
					args[i] = Exceptions.causes(t);
				}
			}
			String s = Processor.formatArrays(format, args);
			Message m = new Message(counter.getAndIncrement(), s, true);
			putMessage(s, m);
			return m;
		} finally {
			processor().signal();
		}
	}

	void putMessage(String s, Message m) {
		ConcurrentHashMap<String, Message> current;
		do {
			current = messages;
			current.putIfAbsent(s, m);
		} while (current != messages);
	}

	SetLocation warning(String format, Object... args) {
		String s = Processor.formatArrays(format, args);
		Message m = new Message(counter.getAndIncrement(), s, false);
		putMessage(s, m);
		return m;
	}

	void getInfo(Reporter from, String prefix) {
		String actualPrefix = prefix == null ? processor().getBase() + " :" : prefix;

		MessageReporter other;
		if (from instanceof Processor processor) {
			other = processor.reporter;
		} else if (from instanceof MessageReporter mr) {
			other = mr;
		} else {
			// backward compatible reporters
			addAll(true, actualPrefix, from);
			addAll(false, actualPrefix, from);
			from.getErrors()
				.clear();
			from.getWarnings()
				.clear();
			return;
		}

		ConcurrentHashMap<String, Message> older = other.clear();
		older.values()
			.stream()
			.sorted()
			.map(m -> new Message(counter.getAndIncrement(), m, actualPrefix))
			.forEach(m -> {
				putMessage(m.message, m);
			});
	}

	/*
	 * for backward compatible reporters
	 */
	void addAll(boolean error, String prefix, Reporter reporter) {
		for (String message : new ArrayList<>(error ? reporter.getErrors() : reporter.getWarnings())) {
			String newMessage = prefix.isEmpty() ? message : prefix.concat(message);
			Message m = new Message(counter.getAndIncrement(), newMessage, error);
			Location location = reporter.getLocation(message);
			if (location != null)
				location.to(m);
			putMessage(newMessage, m);
		}
	}

	ConcurrentHashMap<String, Message> clear() {
		ConcurrentHashMap<String, Message> previous = messages;
		messages = new ConcurrentHashMap<>();
		return previous;
	}

	Processor processor() {
		return this.processor.current();
	}

	Message remove(String message) {
		return messages.remove(message);
	}
}
