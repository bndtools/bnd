package aQute.bnd.osgi;

import static java.util.stream.Collectors.toCollection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;

import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.FilterParser.ExpressionVisitor;
import aQute.bnd.osgi.resource.FilterParser.Op;
import aQute.bnd.osgi.resource.FilterParser.Or;
import aQute.bnd.osgi.resource.FilterParser.PatternExpression;
import aQute.bnd.osgi.resource.FilterParser.SimpleExpression;
import aQute.lib.strings.Strings;

public class PermissionGenerator {
	public enum Parameter {
		ADMIN {
			@Override
			public void generate(StringBuilder sb, Builder builder) {
				sb.append("(org.osgi.framework.AdminPermission)\n");
			}
		},
		CAPABILITIES {
			@Override
			public void generate(StringBuilder sb, Builder builder) {
				for (String namespace : builder.getProvideCapability()
					.keySet()) {
					if (!Processor.isDuplicate(namespace)) {
						sb.append("(org.osgi.framework.CapabilityPermission \"")
							.append(namespace)
							.append("\" \"provide\")\n");
					}
				}
				for (String namespace : builder.getRequireCapability()
					.keySet()) {
					if (!Processor.isDuplicate(namespace)) {
						sb.append("(org.osgi.framework.CapabilityPermission \"")
							.append(namespace)
							.append("\" \"require\")\n");
					}
				}
			}
		},
		PACKAGES {
			@Override
			public void generate(StringBuilder sb, Builder builder) {
				if (builder.getImports() != null) {
					for (PackageRef imp : builder.getImports()
						.keySet()) {
						if (!imp.isJava()) {
							sb.append("(org.osgi.framework.PackagePermission \"");
							sb.append(imp);
							sb.append("\" \"import\")\n");
						}
					}
				}
				if (builder.getExports() != null) {
					for (PackageRef exp : builder.getExports()
						.keySet()) {
						sb.append("(org.osgi.framework.PackagePermission \"");
						sb.append(exp);
						sb.append("\" \"export\")\n");
					}
				}
			}
		},
		SERVICES {
			@Override
			public void generate(StringBuilder sb, Builder builder) {
				for (String declaredService : getDeclaredServices(builder)) {
					sb.append("(org.osgi.framework.ServicePermission \"")
						.append(declaredService)
						.append("\" \"register\")\n");
				}
				for (String referencedService : getReferencedServices(builder)) {
					sb.append("(org.osgi.framework.ServicePermission \"")
						.append(referencedService)
						.append("\" \"get\")\n");
				}
			}
		};

		public abstract void generate(StringBuilder sb, Builder builder);
	}

	public static final String KEY = "permissions";

	public static Set<String> getDeclaredServices(Builder builder) {
		Set<String> declaredServices = builder.getProvideCapability()
			.stream()
			.filterKey(key -> Processor.removeDuplicateMarker(key)
				.equals(ServiceNamespace.SERVICE_NAMESPACE))
			.values()
			.map(attrs -> attrs.get(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE))
			.filter(Objects::nonNull)
			.flatMap(Strings::splitAsStream)
			.collect(toCollection(TreeSet::new));
		return declaredServices;
	}

	private static final String	MATCH_ALL		= "*";
	private static final String	VALID_WILDCARD	= ".*";

	static final class FindReferencedServices extends ExpressionVisitor<Set<String>> {

		public FindReferencedServices() {
			super(Collections.emptySet());
		}

		@Override
		public Set<String> visit(SimpleExpression expr) {
			if ("objectClass".equals(expr.getKey())) {
				if (expr.getOp() == Op.EQUAL) {
					String v = expr.getValue();

					if (!v.contains(MATCH_ALL) || v.equals(MATCH_ALL)) {
						return Collections.singleton(v);
					} else if (v.endsWith(VALID_WILDCARD) && !v.substring(0, v.length() - 2)
						.contains(MATCH_ALL)) {
						return Collections.singleton(v);
					} else {
						// All other matchings have no meaning to us, the user
						// should generate them by hand
						return Collections.emptySet();
					}
				} else {
					// All other matchings have no meaning, so match nothing
					return Collections.emptySet();
				}
			}
			return Collections.emptySet();
		}

		@Override
		public Set<String> visit(PatternExpression expr) {
			// Simply use the default visit method for simple expressions, since
			// the patterns are also supported in the permissions
			return visit((SimpleExpression) expr);
		}

		@Override
		public Set<String> visit(Or expr) {
			Set<String> matches = new HashSet<>();
			for (Expression expression : expr.getExpressions()) {
				matches.addAll(expression.visit(this));
			}
			return matches;
		}

		@Override
		public Set<String> visitTrue() {
			return Collections.singleton(MATCH_ALL);
		}
	}

	public static Set<String> getReferencedServices(Builder builder) {
		Set<String> referencedServices = builder.getRequireCapability()
			.stream()
			.filterKey(key -> Processor.removeDuplicateMarker(key)
				.equals(ServiceNamespace.SERVICE_NAMESPACE))
			.values()
			.map(attrs -> attrs.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE + ":"))
			.filter(Strings::nonNullOrEmpty)
			.flatMap(filter -> new FilterParser().parse(filter)
				.visit(new FindReferencedServices())
				.stream())
			.collect(toCollection(TreeSet::new));

		if (referencedServices.contains(MATCH_ALL)) {
			return Collections.singleton(MATCH_ALL);
		}
		return referencedServices;
	}

	private static EnumSet<Parameter> parseParams(Builder builder, final String... args) {
		EnumSet<Parameter> parameters = EnumSet.noneOf(Parameter.class);
		// Skip the key name, so start at index 1
		for (int ix = 1; ix < args.length; ix++) {
			String name = args[ix].toUpperCase();
			try {
				parameters.add(Parameter.valueOf(name));
			} catch (IllegalArgumentException ex) {
				builder.error("Could not parse argument for ${permissions}: %s", args[ix]);
			}
		}
		return parameters;
	}

	private final Builder			builder;
	private final Set<Parameter>	parameters;

	public PermissionGenerator(Builder builder, final String... args) {
		assert args.length > 0 && KEY.equals(args[0]);
		this.builder = builder;
		this.parameters = parseParams(builder, args);
	}

	public String generate() {
		StringBuilder sb = new StringBuilder();

		for (Parameter param : parameters) {
			param.generate(sb, builder);
		}

		return sb.toString();
	}
}
