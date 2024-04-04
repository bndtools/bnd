package aQute.bnd.wstemplates;

import static aQute.libg.re.Catalog.cc;
import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.opt;
import static aQute.libg.re.Catalog.set;
import static aQute.libg.re.Catalog.some;

import java.net.URI;
import java.util.Map;

import aQute.libg.re.RE;

/**
 * The ID of a template. This is either an org/repo/path#ref pattern or a uri to
 * a zip file.
 */
public record TemplateID(String organisation, String repository, String path, String ref, String other)
	implements Comparable<TemplateID> {

	final static RE SEGMENT_P = some(cc("\\d\\w_.-"));
	final static RE REF_P = some(cc("\\d\\w_.-/"));
	final static RE PATH_P = opt( //
		g("org", SEGMENT_P), opt(lit("/"), g("repo", SEGMENT_P), //
			g("path", set(lit("/"), SEGMENT_P)), opt(lit("/"))),
		opt(lit("#"), g("branch", REF_P)) //
	);
	final static URI ROOT = URI.create("https://github.com/bndtools/workspace#master");

	@Override
	public int compareTo(TemplateID o) {
		if (other != null)
			return other.compareTo(o.other);
		int n = organisation.compareTo(o.organisation);
		if (n != 0)
			return n;
		n = repository.compareTo(o.repository);
		if (n != 0)
			return n;
		n = path.compareTo(o.path);
		if (n != 0)
			return n;
		n = ref.compareTo(o.ref);
		return n;
	}

	/**
	 * Return the URI.
	 */
	public URI uri() {
		String uri = this.other;
		if (uri == null) {
			uri = "https://github.com/" + organisation + "/" + repository + "/archive/" + ref + ".zip";
		}
		return URI.create(uri);
	}

	/**
	 * Parse the id into a Template ID. The default is
	 * `bndtools/workspace#master`. The missing fields are taken from this
	 * default. If the id does not match the pattern, it is assumed to be a URI.
	 *
	 * @param id id or uri
	 * @return a TemplateId
	 */
	public static TemplateID from(String id) {
		return PATH_P.matches(id)
			.map(match -> {
				Map<String, String> vs = match.getGroupValues();
				String org = vs.getOrDefault("org", "bndtools");
				String repo = vs.getOrDefault("repo", "workspace");
				String path = vs.getOrDefault("path", "");
				if (!path.isEmpty())
					path = path.substring(1);
				String branch = vs.getOrDefault("branch", "master");
				return new TemplateID(org, repo, path, branch, null);
			})
			.orElse(new TemplateID(null, null, "", null, id));
	}

}
