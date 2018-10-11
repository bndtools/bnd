---
layout: default
title: bsn2url 
summary: From a set of bsns, create a list of urls if found in the repo                                 
---

## Description

{{page.summary}}

## Synopsis

## Options

## Examples

	/**
	 * From a set of bsns, create a list of urls
	 */

	interface Bsn2UrlOptions extends projectOptions {

	}

	static Pattern	LINE_P	= Pattern.compile("\\s*(([^\\s]#|[^#])+)(\\s*#.*)?");

	public void _bsn2url(Bsn2UrlOptions opts) throws Exception {
		Project p = getProject(opts.project());

		if (p == null) {
			error("You need to be in a project or specify the project with -p/--project");
			return;
		}

		MultiMap<String,Version> revisions = new MultiMap<String,Version>();

		for (RepositoryPlugin repo : p.getPlugins(RepositoryPlugin.class)) {
			if (!(repo instanceof InfoRepository))
				continue;

			for (String bsn : repo.list(null)) {
				revisions.addAll(bsn, repo.versions(bsn));
			}
		}

		for (List<Version> versions : revisions.values()) {
			Collections.sort(versions, Collections.reverseOrder());
		}

		List<String> files = opts._();

		for (String f : files) {
			BufferedReader r = IO.reader(getFile(f));
			try {
				String line;
				nextLine: while ((line = r.readLine()) != null) {
					Matcher matcher = LINE_P.matcher(line);
					if (!matcher.matches())
						continue nextLine;

					line = matcher.group(1);

					Parameters bundles = new Parameters(line);
					for (Map.Entry<String,Attrs> entry : bundles.entrySet()) {

						String bsn = entry.getKey();
						VersionRange range = new VersionRange(entry.getValue().getVersion());

						List<Version> versions = revisions.get(bsn);
						if (versions == null) {
							error("No for versions for " + bsn);
							break nextLine;
						}

						for (Version version : versions) {
							if (range.includes(version)) {

								for (RepositoryPlugin repo : p.getPlugins(RepositoryPlugin.class)) {

									if (!(repo instanceof InfoRepository))
										continue;

									InfoRepository rp = (InfoRepository) repo;
									ResourceDescriptor descriptor = rp.getDescriptor(bsn, version);
									if (descriptor == null) {
										error("Found bundle, but no descriptor %s;version=%s", bsn, version);
										return;
									}

									out.println(descriptor.url + " #" + descriptor.bsn + ";version="
											+ descriptor.version);
								}
							}
						}

					}

				}
			}
			catch (Exception e) {
				error("failed to create url list from file %s : %s", f, e);
			}
			finally {
				r.close();
			}
		}
	}
