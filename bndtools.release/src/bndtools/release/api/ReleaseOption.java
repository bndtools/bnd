package bndtools.release.api;

import java.util.EnumSet;

import bndtools.release.nl.Messages;

public enum ReleaseOption {

	UPDATE(Messages.updateVersions),
	RELEASE(Messages.release),
	UPDATE_RELEASE(Messages.updateVersionsAndRelease);

	private String text;

	private ReleaseOption(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public static ReleaseOption parse(String optionString) {
		try {
			return ReleaseOption.valueOf(optionString);
		} catch (IllegalArgumentException e) {
			// Do nothing
		}

		EnumSet<ReleaseOption> optionSet = EnumSet.allOf(ReleaseOption.class);
		for (ReleaseOption option : optionSet) {
			if (option.getText()
				.equals(optionString)) {
				return option;
			}
		}
		return null;
	}
}
