package aQute.bnd.http;

import static java.util.Objects.requireNonNull;

import aQute.bnd.service.url.TaggedData;

class RetryException extends Exception {
	private static final long	serialVersionUID	= 1L;
	private final TaggedData	tag;

	RetryException(TaggedData tag, Throwable cause) {
		super(cause);
		this.tag = requireNonNull(tag);
	}

	RetryException(TaggedData tag, String message) {
		super(message);
		this.tag = requireNonNull(tag);
	}

	TaggedData getTag() {
		return tag;
	}
}
