---
layout: default
class: Analyzer
title: sha1 ';' RESOURCE
summary: The SHA-1 digest of an existing resource in the JAR
---

	/**
	 * SHA1 macro
	 */

	static String	_sha1Help	= "${sha1;path}";

	public String _sha1(String args[]) throws Exception {
		Macro.verifyCommand(args, _sha1Help, new Pattern[] {
				null, null, Pattern.compile("base64|hex")
		}, 2, 3);
		Digester<SHA1> digester = SHA1.getDigester();
		Resource r = dot.getResource(args[1]);
		if (r == null)
			throw new FileNotFoundException("From sha1, not found " + args[1]);

		IO.copy(r.openInputStream(), digester);
		return Base64.encodeBase64(digester.digest().digest());
	}
