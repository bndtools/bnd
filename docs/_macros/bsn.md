---
layout: default
class: Analyzer
title: bsn
summary: Provide the current bsn when a JAR is generated. This can differ from the Project's bsn when there are sub-bundles.
---

	public String _bsn(String args[]) {
		return getBsn();
	}
