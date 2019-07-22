package aQute.bnd.osgi.resource;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.strings.Strings;

public class FilterParser {
	final Map<String, Expression> cache = new HashMap<>();

	public enum Op {
		GREATER(">"),
		GREATER_OR_EQUAL(">="),
		LESS("<"),
		LESS_OR_EQUAL("<="),
		EQUAL("="),
		NOT_EQUAL("!="),
		RANGE("..");

		private String symbol;

		Op(String s) {
			this.symbol = s;
		}

		public Op not() {
			switch (this) {
				case GREATER :
					return LESS_OR_EQUAL;
				case GREATER_OR_EQUAL :
					return LESS;
				case LESS :
					return GREATER_OR_EQUAL;
				case LESS_OR_EQUAL :
					return GREATER;
				case EQUAL :
					return NOT_EQUAL;
				case NOT_EQUAL :
					return EQUAL;

				default :
					return null;
			}
		}

		@Override
		public String toString() {
			return symbol;
		}
	}

	public static abstract class Expression {
		static Expression	TRUE	= new Expression() {

										@Override
										public boolean eval(Map<String, ?> map) {
											return true;
										}

										@Override
										Expression not() {
											return FALSE;
										}

										@Override
										public <T> T visit(ExpressionVisitor<T> visitor) {
											return visitor.visitTrue();
										}

										@Override
										void toString(StringBuilder sb) {
											sb.append("true");
										}
									};
		static Expression	FALSE	= new Expression() {

										@Override
										public boolean eval(Map<String, ?> map) {
											return false;
										}

										@Override
										public <T> T visit(ExpressionVisitor<T> visitor) {
											return visitor.visitFalse();
										}

										@Override
										Expression not() {
											return TRUE;
										}

										@Override
										void toString(StringBuilder sb) {
											sb.append("false");
										}
									};

		public abstract boolean eval(Map<String, ?> map);

		public abstract <T> T visit(ExpressionVisitor<T> visitor);

		Expression not() {
			return null;
		}

		abstract void toString(StringBuilder sb);

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}

		public String query() {
			return null;
		}
	}

	public static class RangeExpression extends SimpleExpression {
		final SimpleExpression	low;
		final SimpleExpression	high;

		public RangeExpression(String key, SimpleExpression low, SimpleExpression high) {
			super(key, Op.RANGE, null);
			this.low = low;
			this.high = high;
		}

		@Override
		protected boolean eval(Object scalar) {
			return (low == null || low.eval(scalar)) && (high == null || high.eval(scalar));
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		static Expression make(String key, SimpleExpression low, SimpleExpression high) {
			if (key.contains("version")) {
				try {
					Version a = Version.parseVersion(low.value);
					Version b = Version.parseVersion(high.value);
					if (a.compareTo(b) > 0)
						return FALSE;

					if (a.equals(Version.LOWEST) && b.equals(Version.HIGHEST))
						return TRUE;

					if (b.equals(Version.HIGHEST))
						return low;
					if (a.equals(Version.LOWEST))
						return high;

				} catch (Exception e) {
					// ignore, might not be a version
				}
			}
			return new RangeExpression(key, low, high);
		}

		public String getRangeString() {
			StringBuilder sb = new StringBuilder();
			if (low != null) {
				if (high == null)
					sb.append(low.value);
				else {
					if (low.op == Op.GREATER)
						sb.append("(");
					else
						sb.append("[");
					sb.append(low.value);
				}
			}
			if (high != null) {
				sb.append(",");
				if (low == null) {
					sb.append("[0.0.0,");
				}
				sb.append(high.value);
				if (high.op == Op.LESS)
					sb.append(")");
				else
					sb.append("]");
			}
			return sb.toString();
		}

		@Override
		public void toString(StringBuilder sb) {
			sb.append(key)
				.append("=")
				.append(getRangeString());
		}

		public SimpleExpression getLow() {
			return low;
		}

		public SimpleExpression getHigh() {
			return high;
		}
	}

	public static class SimpleExpression extends Expression {
		final Op			op;
		final String		key;
		final String		value;
		transient Object	cached;

		public SimpleExpression(String key, Op op, String value) {
			this.key = key;
			this.op = op;
			this.value = value;
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			Object target = map.get(key);
			if (target instanceof Iterable) {
				for (Object scalar : (Iterable<?>) target) {
					if (eval(scalar))
						return true;
				}
				return false;
			} else if (target.getClass()
				.isArray()) {
				int l = Array.getLength(target);
				for (int i = 0; i < l; i++) {
					if (eval(Array.get(target, i)))
						return true;
				}
				return false;
			} else {
				return eval(target);
			}
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		protected boolean eval(Object scalar) {
			if (cached == null || cached.getClass() != scalar.getClass()) {
				Class<?> scalarClass = scalar.getClass();
				if (scalarClass == String.class)
					cached = value;
				else if (scalarClass == Byte.class)
					cached = Byte.parseByte(value);
				else if (scalarClass == Short.class)
					cached = Short.parseShort(value);
				else if (scalarClass == Integer.class)
					cached = Integer.parseInt(value);
				else if (scalarClass == Long.class)
					cached = Long.parseLong(value);
				else if (scalarClass == Float.class)
					cached = Float.parseFloat(value);
				else if (scalarClass == Double.class)
					cached = Double.parseDouble(value);
				else if (scalarClass == Character.class)
					cached = value;
				else {
					try {
						MethodHandle mh;
						try {
							mh = publicLookup().findStatic(scalarClass, "valueOf",
								methodType(scalarClass, String.class));
						} catch (NoSuchMethodException | IllegalAccessException e) {
							mh = publicLookup().findConstructor(scalarClass, methodType(void.class, String.class));
						}
						cached = mh.invoke(value);
					} catch (Error e) {
						throw e;
					} catch (Throwable e) {
						cached = value;
					}
				}
			}
			if (op == Op.EQUAL)
				return cached == scalar || cached.equals(scalar);
			if (op == Op.NOT_EQUAL)
				return !cached.equals(scalar);

			if (cached instanceof Comparable<?>) {
				@SuppressWarnings("unchecked")
				int result = ((Comparable<Object>) scalar).compareTo(cached);
				switch (op) {
					case LESS :
						return result < 0;
					case LESS_OR_EQUAL :
						return result <= 0;
					case GREATER :
						return result > 0;
					case GREATER_OR_EQUAL :
						return result >= 0;
					default :
						break;
				}
			}
			return false;
		}

		static Expression make(String key, Op op, String value) {
			if (op == Op.EQUAL) {
				if ("osgi.wiring.bundle".equals(key))
					return new BundleExpression(value);
				else if ("osgi.wiring.host".equals(key))
					return new HostExpression(value);
				else if ("osgi.wiring.package".equals(key))
					return new PackageExpression(value);
				else if ("osgi.identity".equals(key))
					return new IdentityExpression(value);
			}
			return new SimpleExpression(key, op, value);

		}

		@Override
		Expression not() {
			Op alt = op.not();
			if (alt == null)
				return null;

			return new SimpleExpression(key, alt, value);
		}

		@Override
		public void toString(StringBuilder sb) {
			sb.append(key)
				.append(op.toString())
				.append(value);
		}

		@Override
		public String query() {
			return value;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

		public Op getOp() {
			return op;
		}

	}

	public abstract static class WithRangeExpression extends Expression {
		RangeExpression range;

		@Override
		public boolean eval(Map<String, ?> map) {
			return range == null || range.eval(map);
		}

		@Override
		void toString(StringBuilder sb) {
			if (range == null)
				return;

			sb.append("; ");
			range.toString(sb);
		}

		public RangeExpression getRangeExpression() {
			return range;
		}

		public abstract String printExcludingRange();

	}

	public static class PackageExpression extends WithRangeExpression {
		final String packageName;

		public PackageExpression(String value) {
			this.packageName = value;
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			String p = (String) map.get("osgi.wiring.package");
			if (p == null)
				return false;

			return packageName.equals(p) && super.eval(map);
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(packageName);
			super.toString(sb);
		}

		public String getPackageName() {
			return packageName;
		}

		@Override
		public String query() {
			return "p:" + packageName;
		}

		@Override
		public String printExcludingRange() {
			return packageName;
		}
	}

	public static class HostExpression extends WithRangeExpression {
		final String hostName;

		public HostExpression(String value) {
			this.hostName = value;
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			String p = (String) map.get("osgi.wiring.host");
			if (p == null)
				return false;

			return hostName.equals(p) && super.eval(map);
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(hostName);
			super.toString(sb);
		}

		public String getHostName() {
			return hostName;
		}

		@Override
		public String query() {
			return "bsn:" + hostName;
		}

		@Override
		public String printExcludingRange() {
			return hostName;
		}
	}

	public static class BundleExpression extends WithRangeExpression {
		final String bundleName;

		public BundleExpression(String value) {
			this.bundleName = value;
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			String p = (String) map.get("osgi.wiring.bundle");
			if (p == null)
				return false;

			return bundleName.equals(p) && super.eval(map);
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(bundleName);
			super.toString(sb);
		}

		@Override
		public String query() {
			return "bsn:" + bundleName;
		}

		@Override
		public String printExcludingRange() {
			return bundleName;
		}

	}

	public static class IdentityExpression extends WithRangeExpression {
		final String identity;

		public IdentityExpression(String value) {
			this.identity = value;
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			String p = (String) map.get("osgi.identity");
			if (p == null)
				return false;

			return identity.equals(p);
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(identity);
			super.toString(sb);
		}

		public String getSymbolicName() {
			return identity;
		}

		@Override
		public String query() {
			return "bsn:" + identity;
		}

		@Override
		public String printExcludingRange() {
			return identity;
		}
	}

	public static abstract class SubExpression extends Expression {
		Expression[] expressions;

		@Override
		void toString(StringBuilder sb) {
			for (Expression e : expressions) {
				sb.append("(");
				e.toString(sb);
				sb.append(")");
			}
		}

		public Expression[] getExpressions() {
			return expressions;
		}

		@Override
		public String query() {
			if (expressions == null || expressions.length == 0)
				return null;

			if (expressions[0] instanceof WithRangeExpression) {
				return expressions[0].query();
			}

			List<String> words = new ArrayList<>();
			for (Expression e : expressions) {
				String query = e.query();
				if (query != null)
					words.add(query);
			}

			return Strings.join(" ", words);
		}

	}

	public static class And extends SubExpression {
		private And(List<Expression> exprs) {
			this.expressions = exprs.toArray(new Expression[0]);
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			for (Expression e : expressions) {
				if (!e.eval(map))
					return false;
			}
			return true;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		static Expression make(List<Expression> exprs) {

			for (Iterator<Expression> i = exprs.iterator(); i.hasNext();) {
				Expression e = i.next();
				if (e == FALSE)
					return FALSE;
				if (e == TRUE)
					i.remove();
			}
			if (exprs.isEmpty())
				return TRUE;

			SimpleExpression lower = null;
			SimpleExpression higher = null;
			WithRangeExpression wre = null;

			for (Expression e : exprs) {
				if (e instanceof WithRangeExpression) {
					wre = (WithRangeExpression) e;
				} else if (e instanceof SimpleExpression) {
					SimpleExpression se = (SimpleExpression) e;

					if (se.key.equals("version") || se.key.equals("bundle-version")) {
						if (se.op == Op.GREATER || se.op == Op.GREATER_OR_EQUAL)
							lower = se;
						else if (se.op == Op.LESS || se.op == Op.LESS_OR_EQUAL)
							higher = se;
					}
				}
			}

			RangeExpression range = null;
			if (lower != null || higher != null) {
				if (lower != null && higher != null) {
					exprs.remove(lower);
					exprs.remove(higher);
					range = new RangeExpression("version", lower, higher);
				} else if (lower != null && lower.op == Op.GREATER_OR_EQUAL && higher == null) {
					exprs.remove(lower);
					range = new RangeExpression("version", lower, null);
				}
			}

			if (range != null) {
				if (wre != null)
					wre.range = range;
				else
					exprs.add(range);
			}

			if (exprs.size() == 1)
				return exprs.get(0);

			return new And(exprs);
		}

		@Override
		public void toString(StringBuilder sb) {
			if (expressions != null && expressions.length > 0) {
				if (expressions[0] instanceof WithRangeExpression) {
					sb.append(expressions[0]);

					for (int i = 1; i < expressions.length; i++) {
						sb.append("; ");
						expressions[i].toString(sb);
					}
					return;
				}
			}
			sb.append("&");
			super.toString(sb);
		}

	}

	public static class Or extends SubExpression {
		private Or(List<Expression> exprs) {
			this.expressions = exprs.toArray(new Expression[0]);
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			for (Expression e : expressions) {
				if (e.eval(map))
					return true;
			}
			return false;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		static Expression make(List<Expression> exprs) {

			for (Iterator<Expression> i = exprs.iterator(); i.hasNext();) {
				Expression e = i.next();
				if (e == TRUE)
					return TRUE;
				if (e == FALSE)
					i.remove();
			}
			if (exprs.isEmpty())
				return FALSE;

			if (exprs.size() == 1)
				return exprs.get(0);

			return new Or(exprs);
		}

		@Override
		public void toString(StringBuilder sb) {
			sb.append("|");
			super.toString(sb);
		}
	}

	public static class Not extends Expression {
		Expression expr;

		private Not(Expression expr) {
			this.expr = expr;
		}

		@Override
		public boolean eval(Map<String, ?> map) {
			return !expr.eval(map);
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		public static Expression make(Expression expr) {
			if (expr == TRUE)
				return FALSE;
			if (expr == FALSE)
				return TRUE;

			Expression notexpr = expr.not();
			if (notexpr != null)
				return notexpr;

			return new Not(expr);
		}

		@Override
		Expression not() {
			return expr;
		}

		@Override
		public void toString(StringBuilder sb) {
			sb.append("!(");
			expr.toString(sb);
			sb.append(")");
		}
	}

	public static class PatternExpression extends SimpleExpression {
		final Pattern pattern;

		public PatternExpression(String key, String value) {
			super(key, Op.EQUAL, value);

			value = Pattern.quote(value);
			this.pattern = Pattern.compile(value.replace("\\*", ".*"));
		}

		@Override
		protected boolean eval(Object scalar) {
			if (scalar instanceof String)
				return pattern.matcher((String) scalar)
					.matches();
			else
				return false;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

	}

	public static class ApproximateExpression extends SimpleExpression {
		public ApproximateExpression(String key, String value) {
			super(key, Op.EQUAL, value);
		}

		@Override
		protected boolean eval(Object scalar) {
			if (scalar instanceof String) {
				return ((String) scalar).trim()
					.equalsIgnoreCase(value);
			} else
				return false;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}
	}

	public static abstract class ExpressionVisitor<T> {
		private final T defaultValue;

		public ExpressionVisitor(T defaultValue) {
			this.defaultValue = defaultValue;
		}

		public T visit(RangeExpression expr) {
			return defaultValue;
		}

		public T visit(SimpleExpression expr) {
			return defaultValue;
		}

		public T visit(PackageExpression expr) {
			return defaultValue;
		}

		public T visit(HostExpression expr) {
			return defaultValue;
		}

		public T visit(BundleExpression expr) {
			return defaultValue;
		}

		public T visit(IdentityExpression expr) {
			return defaultValue;
		}

		public T visit(And expr) {
			return defaultValue;
		}

		public T visit(Or expr) {
			return defaultValue;
		}

		public T visit(Not expr) {
			return defaultValue;
		}

		public T visit(PatternExpression expr) {
			return defaultValue;
		}

		public T visit(ApproximateExpression expr) {
			return defaultValue;
		}

		public T visitTrue() {
			return defaultValue;
		}

		public T visitFalse() {
			return defaultValue;
		}
	}

	static class Rover {
		String	s;
		int		n	= 0;

		char next() {
			return s.charAt(n++);
		}

		char wsNext() {
			ws();
			return next();
		}

		char current() {
			return s.charAt(n);
		}

		void ws() {
			while (Character.isWhitespace(current()))
				n++;
		}

		String findExpr() {
			int nn = n;
			int level = 0;
			while (nn < s.length()) {
				char c = s.charAt(nn++);
				switch (c) {
					case '(' :
						level++;
						break;

					case '\\' :
						nn++;
						break;

					case ')' :
						level--;
						if (level == 0)
							return s.substring(n, nn);
				}
			}
			// bad expression
			return "";
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(s)
				.append("\n");
			for (int i = 0; i < n; i++)
				sb.append(" ");
			sb.append("|");
			return sb.toString();
		}

		private boolean isOpChar(char s) {
			return s == '=' || s == '~' || s == '>' || s == '<' || s == '(' || s == ')';
		}

		String getKey() {
			int n = this.n;
			while (!isOpChar(current()))
				next();
			return s.substring(n, this.n)
				.trim();
		}

		String getValue() {
			int n = this.n;
			while (current() != ')') {
				char c = next();
				if (c == '\\') {
					// TODO verify if they escape other chars?
					this.n++;
				}
			}
			return s.substring(n, this.n);
		}
	}

	public Expression parse(String s) {
		Rover rover = new Rover();
		rover.s = s;
		rover.n = 0;
		return parse(rover);
	}

	public Expression parse(Requirement req) {
		String f = req.getDirectives()
			.get("filter");
		if (f == null)
			return Expression.FALSE;
		return parse(f);
	}

	public Expression parse(Rover rover) {
		try {
			String cacheKey = rover.findExpr();
			Expression e = cache.get(cacheKey);
			if (e != null) {
				rover.n += cacheKey.length();
				return e;
			}

			rover.ws();
			char c = rover.current();
			if (c != '(')
				throw new IllegalArgumentException("Expression must start with a '('");
			rover.next();
			rover.ws();
			e = parse0(rover);
			rover.ws();
			c = rover.current();

			if (c != ')')
				throw new IllegalArgumentException("Expression must end with a ')'");
			rover.next();
			cache.put(cacheKey, e);
			return e;
		} catch (RuntimeException re) {
			throw new RuntimeException("Parsing failed: " + re.getMessage() + ":\n" + rover + "\n", re);
		}
	}

	Expression parse0(Rover rover) {
		rover.ws();
		switch (rover.next()) {
			case '&' :
				return And.make(parseExprs(rover));

			case '|' :
				return Or.make(parseExprs(rover));

			case '!' :
				return Not.make(parse(rover));

			default :
				rover.n--;
				String key = rover.getKey();
				char s = rover.next();
				if (s == '=') {
					String value = rover.getValue();
					if (value.indexOf('*') >= 0)
						return new PatternExpression(key, value);
					else
						return SimpleExpression.make(key, Op.EQUAL, value);
				}

				char eq = rover.next();
				if (eq != '=')
					throw new IllegalArgumentException("Expected an = after " + s);

				switch (s) {
					case '~' :
						return new ApproximateExpression(key, rover.getValue());
					case '>' :
						return SimpleExpression.make(key, Op.GREATER_OR_EQUAL, rover.getValue());
					case '<' :
						return SimpleExpression.make(key, Op.LESS_OR_EQUAL, rover.getValue());
					default :
						throw new IllegalArgumentException("Expected '~=', '>=', '<='");
				}
		}
	}

	private List<Expression> parseExprs(Rover rover) {
		ArrayList<Expression> exprs = new ArrayList<>();
		rover.ws();
		while (rover.current() == '(') {
			Expression expr = parse(rover);
			exprs.add(expr);
			rover.ws();
		}
		return exprs;
	}

	public static String namespaceToCategory(String namespace) {
		String result;

		if ("osgi.wiring.package".equals(namespace)) {
			result = "Import-Package";
		} else if ("osgi.wiring.bundle".equals(namespace)) {
			result = "Require-Bundle";
		} else if ("osgi.wiring.host".equals(namespace)) {
			result = "Fragment-Host";
		} else if ("osgi.identity".equals(namespace)) {
			result = "ID";
		} else if ("osgi.content".equals(namespace)) {
			result = "Content";
		} else if ("osgi.extender".equals(namespace)) {
			result = "Extender";
		} else if ("osgi.service".equals(namespace)) {
			result = "Service";
		} else if ("osgi.contract".equals(namespace)) {
			return "Contract";
		} else {
			result = namespace;
		}

		return result;
	}

	public static String toString(Requirement r) {
		try {
			StringBuilder sb = new StringBuilder();
			String category = namespaceToCategory(r.getNamespace());
			if (category != null && category.length() > 0)
				sb.append(namespaceToCategory(category))
					.append(": ");

			FilterParser fp = new FilterParser();
			String filter = r.getDirectives()
				.get("filter");
			if (filter == null)
				sb.append("<no filter>");
			else {
				Expression parse = fp.parse(filter);
				sb.append(parse);
			}
			return sb.toString();
		} catch (Exception e) {
			return Exceptions.toString(e);
		}
	}

	public String simple(Resource resource) {
		if (resource == null)
			return "<>";

		List<Capability> capabilities = resource.getCapabilities("osgi.identity");
		if (capabilities.isEmpty())
			return resource.toString();

		Capability c = capabilities.get(0);
		String bsn = (String) c.getAttributes()
			.get("osgi.identity");
		Object version = c.getAttributes()
			.get("version");
		if (version == null)
			return bsn;
		else
			return bsn + ";version=" + version;
	}
}
