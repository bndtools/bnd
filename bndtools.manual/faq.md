---
title: Bndtools FAQ
description: Frequently Asked Questions
author: Neil Bartlett
---

How Do I Add Bundles to the Repository?
---------------------------------------

There are several ways to add external bundles into your repository in order to make them available in your projects. The easiest way is to drag-and-drop from your native file management application (i.e. Windows Explorer, Mac Finder, etc) into the **Repositories** view in Bndtools. Note that you need to drop it onto a specific repository entry: usually the "Local Repository". Note that you can multi-select many bundle files and drag/drop them at the same time.

![](/images/faq/01.png)

Importing the bundles this way is better than directly modifying the contents of the `cnf/repo` directory, since Bndtools is immediately aware of the new bundles and regenerates the OBR index for the repository.