---
layout: default
class: Processor
title: random
summary: Generate a random string, which is guaranteed to be a valid Java identifier
---

	/**
	 * Generate a random string, which is guaranteed to be a valid Java
	 * identifier (first character is an ASCII letter, subsequent characters are
	 * ASCII letters or numbers). Takes an optional parameter for the length of
	 * string to generate; default is 8 characters.
	 */
	public String _random(String[] args) {
		int numchars = 8;
		if (args.length > 1) {
			try {
				numchars = Integer.parseInt(args[1]);
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid character count parameter in ${random} macro.");
			}
		}

		synchronized (Processor.class) {
			if (random == null)
				random = new Random();
		}

		char[] letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		char[] alphanums = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

		char[] array = new char[numchars];
		for (int i = 0; i < numchars; i++) {
			char c;
			if (i == 0)
				c = letters[random.nextInt(letters.length)];
			else
				c = alphanums[random.nextInt(alphanums.length)];
			array[i] = c;
		}

		return new String(array);
	}
