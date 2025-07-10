---
layout: default
title: -deploy
class: Project
summary: |
   Deploy the current project to a repository through Deploy plugins (e.g. MavenDeploy plugin)
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-deploy=mavenrepo`

- Values: `${repos}`

- Pattern: `.*`

<!-- Manual content from: ext/deploy.md --><br /><br />

NEEDS WORK?

	/**
	 * Deploy the current project to a repository
	 *
	 * @throws Exception
	 */
	public void deploy() throws Exception {
		Parameters deploy = new Parameters(getProperty(DEPLOY));
		if (deploy.isEmpty()) {
			warning("Deploying but %s is not set to any repo", DEPLOY);
			return;
		}
		File[] outputs = getBuildFiles();
		for (File output : outputs) {
			for (Deploy d : getPlugins(Deploy.class)) {
				trace("Deploying %s to: %s", output.getName(), d);
				try {
					if (d.deploy(this, output.getName(), new BufferedInputStream(new FileInputStream(output))))
						trace("deployed %s successfully to %s", output, d);
				}
				catch (Exception e) {
					msgs.Deploying(e);
				}
			}
		}
	}
