---
layout: default
class: Builder
title: githead
summary: Get the head commit number. Look for a .git/HEAD file, going up in the file hierarchy. Then get this file, and resolve any symbolic reference.
---


	/**
	 * #388 Manifest header to get GIT head Get the head commit number. Look
	 * for a .git/HEAD file, going up in the file hierarchy. Then get this file,
	 * and resolve any symbolic reference.
	 *
	 * @throws IOException
	 */
	static Pattern	GITREF	= Pattern.compile("ref:\\s*(refs/(heads|tags|remotes)/([^\\s]+))\\s*");

	static String	_githeadHelp	= "${githead}, provide the SHA for the current git head";

	public String _githead(String[] args) throws IOException {
		Macro.verifyCommand(args, _githeadHelp, null, 1, 1);

		//
		// Locate the .git directory
		//

		File rover = getBase();
		while (rover !=null && rover.isDirectory()) {
			File headFile = IO.getFile(rover, ".git/HEAD");
			if (headFile.isFile()) {
				//
				// The head is either a symref (ref: refs/(heads|tags|remotes)/<name>)
				//
				String head = IO.collect(headFile).trim();
				if (!Hex.isHex(head)) {
					//
					// Should be a symref
					//
					Matcher m = GITREF.matcher(head);
					if (m.matches()) {

						// so the commit is in the following path

						head = IO.collect(IO.getFile(rover, ".git/" + m.group(1)));
					}
					else {
						error("Git repo seems corrupt. It exists, find the HEAD but the content is neither hex nor a sym-ref: %s",
								head);
					}
				}
				return head.trim().toUpperCase();
			}
			rover = rover.getParentFile();
		}
		// Cannot find git directory
		return "";
	}
