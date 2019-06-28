package aQute.bnd.osgi.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.osgi.resource.ResourceUtils;

/**
 * WARNING ! Not tested
 */
public abstract class BaseRepository implements Repository {
	private static final RequirementExpression[]	EMPTY			= new RequirementExpression[0];
	static IdentityExpression						all;
	private final PromiseFactory					promiseFactory	= new PromiseFactory(
		PromiseFactory.inlineExecutor());

	static {
		aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
			IdentityNamespace.IDENTITY_NAMESPACE);
		rb.addFilter("(" + IdentityNamespace.IDENTITY_NAMESPACE + "=*)");
		final Requirement requireAll = rb.synthetic();
		all = () -> requireAll;
	}

	@Override
	public Promise<Collection<Resource>> findProviders(RequirementExpression expression) {
		Set<Resource> providers = new HashSet<>();

		dispatch(expression, providers);

		return promiseFactory.resolved(providers);
	}

	private void dispatch(RequirementExpression expression, Set<Resource> providers) {
		if (expression instanceof IdentityExpression) {
			Map<Requirement, Collection<Capability>> capabilities = findProviders(
				Collections.singleton(((IdentityExpression) expression).getRequirement()));
			for (Collection<Capability> caps : capabilities.values()) {
				for (Capability c : caps)
					providers.add(c.getResource());
			}
		} else if (expression instanceof OrExpression) {
			for (RequirementExpression re : ((OrExpression) expression).getRequirementExpressions())
				dispatch(re, providers);
		} else if (expression instanceof AndExpression) {
			List<RequirementExpression> requirementExpressions = ((AndExpression) expression)
				.getRequirementExpressions();

			if (requirementExpressions.isEmpty())
				return;

			if (requirementExpressions.size() == 1) {
				dispatch(requirementExpressions.get(0), providers);
				return;
			}

			Set<Resource> subset = new HashSet<>();
			dispatch(requirementExpressions.get(0), subset);

			for (int i = 1; i < requirementExpressions.size(); i++) {

				for (Iterator<Resource> it = subset.iterator(); it.hasNext();) {

					Resource resource = it.next();

					RequirementExpression re = requirementExpressions.get(i);
					if (!matches(re, resource)) {

						it.remove();

						if (subset.isEmpty())
							return;
					}
				}
			}
			providers.addAll(subset);
		} else if (expression instanceof NotExpression) {
			Set<Resource> allSet = new HashSet<>();
			dispatch(all, allSet);
			RequirementExpression re = ((NotExpression) expression).getRequirementExpression();
			for (Iterator<Resource> it = allSet.iterator(); it.hasNext();) {
				Resource resource = it.next();
				if (matches(re, resource)) {
					it.remove();
					if (allSet.isEmpty())
						return;
				}
			}
			providers.addAll(allSet);
		} else
			throw new UnsupportedOperationException("Unknown expression type " + expression.getClass());
	}

	private boolean matches(RequirementExpression expression, Resource resource) {
		if (expression instanceof IdentityExpression) {
			Requirement r = ((IdentityExpression) expression).getRequirement();
			return ResourceUtils.matches(r, resource);
		} else if (expression instanceof OrExpression) {
			List<RequirementExpression> res = ((OrExpression) expression).getRequirementExpressions();
			for (RequirementExpression re : res) {
				if (matches(re, resource))
					return true;
			}
			return false;
		} else if (expression instanceof AndExpression) {
			List<RequirementExpression> res = ((AndExpression) expression).getRequirementExpressions();
			for (RequirementExpression re : res) {
				if (!matches(re, resource))
					return false;
			}
			return true;
		} else if (expression instanceof NotExpression) {
			RequirementExpression re = ((NotExpression) expression).getRequirementExpression();
			return !matches(re, resource);
		} else
			throw new UnsupportedOperationException("Unknown expression type " + expression.getClass());
	}

	@Override
	public ExpressionCombiner getExpressionCombiner() {
		return new ExpressionCombiner() {

			@Override
			public OrExpression or(RequirementExpression expr1, RequirementExpression expr2,
				RequirementExpression... moreExprs) {
				final List<RequirementExpression> exprs = combine(expr1, expr2, moreExprs);
				return () -> exprs;
			}

			List<RequirementExpression> combine(RequirementExpression expr1, RequirementExpression expr2,
				RequirementExpression... moreExprs) {
				List<RequirementExpression> exprs = new ArrayList<>();
				exprs = new ArrayList<>();
				exprs.add(expr1);
				exprs.add(expr2);
				Collections.addAll(exprs, moreExprs);

				return Collections.unmodifiableList(exprs);
			}

			@Override
			public OrExpression or(RequirementExpression expr1, RequirementExpression expr2) {
				return or(expr1, expr2, EMPTY);
			}

			@Override
			public NotExpression not(final RequirementExpression expr) {
				return () -> expr;
			}

			@Override
			public IdentityExpression identity(final Requirement req) {
				return () -> req;
			}

			@Override
			public AndExpression and(RequirementExpression expr1, RequirementExpression expr2,
				RequirementExpression... moreExprs) {

				final List<RequirementExpression> exprs = combine(expr1, expr2, moreExprs);

				return () -> exprs;
			}

			@Override
			public AndExpression and(RequirementExpression expr1, RequirementExpression expr2) {
				return and(expr1, expr2, EMPTY);
			}
		};
	}

	@Override
	public RequirementBuilder newRequirementBuilder(String namespace) {
		final aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(namespace);

		return new RequirementBuilder() {

			@Override
			public RequirementBuilder setResource(Resource resource) {
				rb.setResource(resource);
				return this;
			}

			@Override
			public RequirementBuilder setDirectives(Map<String, String> directives) {
				rb.addDirectives(directives);
				return this;
			}

			@Override
			public RequirementBuilder setAttributes(Map<String, Object> attributes) {
				try {
					rb.addAttributes(attributes);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return this;
			}

			@Override
			public IdentityExpression buildExpression() {
				return () -> {
					if (rb.getResource() == null)
						return rb.buildSyntheticRequirement();
					return rb.build();
				};
			}

			@Override
			public Requirement build() {
				if (rb.getResource() == null)
					return rb.buildSyntheticRequirement();
				return rb.build();
			}

			@Override
			public RequirementBuilder addDirective(String name, String value) {
				rb.addDirective(name, value);
				return this;
			}

			@Override
			public RequirementBuilder addAttribute(String name, Object value) {
				try {
					rb.addAttribute(name, value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return this;
			}
		};
	}

}
