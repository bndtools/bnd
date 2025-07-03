---
layout: default
class: Project
title: -resourceonly  BOOLEAN
summary: Ignores warning if the bundle only contains resources and no classes. 
---

	public boolean isNoBundle() {
		return isTrue(getProperty(RESOURCEONLY)) || isTrue(getProperty(NOMANIFEST));
	}

	/**
	 * @return
	 */
	boolean isResourceOnly() {
		return isTrue(getProperty(RESOURCEONLY));
	}

