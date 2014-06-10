package aQute.bnd.osgi.resource;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.resource.*;

import aQute.bnd.version.*;
import aQute.lib.strings.*;

public class FilterParser {
	final Map<String,Expression>	cache	= new HashMap<String,FilterParser.Expression>();

	public enum Op {
		GREATER(">"), GREATER_OR_EQUAL(">="), LESS("<"), LESS_OR_EQUAL("<="), EQUAL("=="), NOT_EQUAL("!="), RANGE("..");

		private String	symbol;

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

		public String toString() {
			return symbol;
		}
	}

	public static abstract class Expression {
		static Expression	TRUE	= new Expression() {

										@Override
										public boolean eval(Map<String,Object> map) {
											return true;
										}

										@Override
										Expression not() {
											return FALSE;
										}

										@Override
										void toString(StringBuilder sb) {
											sb.append("true");
										}
									};
		static Expression	FALSE	= new Expression() {

										@Override
										public boolean eval(Map<String,Object> map) {
											return false;
										}

										@Override
										Expression not() {
											return TRUE;
										}

										void toString(StringBuilder sb) {
											sb.append("false");
										}
									};

		public abstract boolean eval(Map<String,Object> map);

		Expression not() {
			return null;
		}

		abstract void toString(StringBuilder sb);

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

		static Expression make(String key, SimpleExpression low, SimpleExpression high) {
			if (key.indexOf("version") >= 0) {
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

				}
				catch (Exception e) {
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

		public void toString(StringBuilder sb) {
			sb.append(key).append("=").append(getRangeString());
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
		public boolean eval(Map<String,Object> map) {
			Object target = map.get(key);
			if (target instanceof Iterable) {
				for (Object scalar : (Iterable< ? >) target) {
					if (eval(scalar))
						return true;
				}
				return false;
			} else if (target.getClass().isArray()) {
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

		protected boolean eval(Object scalar) {
			if (cached == null || cached.getClass() != scalar.getClass()) {
				Class< ? > scalarClass = scalar.getClass();
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
						Method factory = scalarClass.getMethod("valueOf", String.class);
						cached = factory.invoke(null, value);
					}
					catch (Exception e) {
						Constructor< ? > constructor;
						try {
							constructor = scalarClass.getConstructor(String.class);
							cached = constructor.newInstance(value);
						}
						catch (Exception e1) {
							cached = value;
						}
					}
				}
			}
			if (op == Op.EQUAL)
				return cached == scalar || cached.equals(scalar);
			if (op == Op.NOT_EQUAL)
				return !cached.equals(scalar);

			if (cached instanceof Comparable< ? >) {
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

		Expression not() {
			Op alt = op.not();
			if (alt == null)
				return null;

			return new SimpleExpression(key, alt, value);
		}

		public void toString(StringBuilder sb) {
			sb.append(key).append(op.toString()).append(value);
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
		RangeExpression	range;

		public boolean eval(Map<String,Object> map) {
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
		final String	packageName;

		public PackageExpression(String value) {
			this.packageName = value;
		}

		@Override
		public boolean eval(Map<String,Object> map) {
			String p = (String) map.get("osgi.wiring.package");
			if (p == null)
				return false;

			return packageName.equals(p) && super.eval(map);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(packageName);
			super.toString(sb);
		}

		public String getPackageName() {
			return packageName;
		}

		public String query() {
			return "p:" + packageName;
		}
		
		@Override
		public String printExcludingRange() {
			return packageName;
		}
	}

	public static class HostExpression extends WithRangeExpression {
		final String	hostName;

		public HostExpression(String value) {
			this.hostName = value;
		}

		@Override
		public boolean eval(Map<String,Object> map) {
			String p = (String) map.get("osgi.wiring.host");
			if (p == null)
				return false;

			return hostName.equals(p) && super.eval(map);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(hostName);
			super.toString(sb);
		}

		public String getHostName() {
			return hostName;
		}

		public String query() {
			return "bsn:" + hostName;
		}
		
		@Override
		public String printExcludingRange() {
			return hostName;
		}
	}

	public static class BundleExpression extends WithRangeExpression {
		final String	bundleName;

		public BundleExpression(String value) {
			this.bundleName = value;
		}

		@Override
		public boolean eval(Map<String,Object> map) {
			String p = (String) map.get("osgi.wiring.bundle");
			if (p == null)
				return false;

			return bundleName.equals(p) && super.eval(map);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(bundleName);
			super.toString(sb);
		}

		public String query() {
			return "bsn:" + bundleName;
		}
		
		@Override
		public String printExcludingRange() {
			return bundleName;
		}

	}

	public static class IdentityExpression extends WithRangeExpression {
		final String	identity;

		public IdentityExpression(String value) {
			this.identity = value;
		}

		@Override
		public boolean eval(Map<String,Object> map) {
			String p = (String) map.get("osgi.identity");
			if (p == null)
				return false;

			return identity.equals(p);
		}

		@Override
		void toString(StringBuilder sb) {
			sb.append(identity);
			super.toString(sb);
		}

		public String getSymbolicName() {
			return identity;
		}

		public String query() {
			return "bsn:" + identity;
		}
		
		@Override
		public String printExcludingRange() {
			return identity;
		}
	}

	public static abstract class SubExpression extends Expression {
		Expression[]	expressions;

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

			List<String> words = new ArrayList<String>();
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
			this.expressions = exprs.toArray(new Expression[exprs.size()]);
		}

		public boolean eval(Map<String,Object> map) {
			for (Expression e : expressions) {
				if (!e.eval(map))
					return false;
			}
			return true;
		}

		static Expression make(List<Expression> exprs) {

			for (Iterator<Expression> i = exprs.iterator(); i.hasNext();) {
				Expression e = i.next();
				if (e == FALSE)
					return FALSE;
				if (e == TRUE)
					i.remove();
			}
			if (exprs.size() == 0)
				return TRUE;

			SimpleExpression lower = null;
			SimpleExpression higher = null;
			WithRangeExpression wre = null;

			for (Expression e : exprs) {
				if (e instanceof WithRangeExpression) {
					wre = (WithRangeExpression) e;
				} else if (e instanceof SimpleExpression) {
					SimpleExpression se = (SimpleExpression) e;

					if (se.key.equals("version")) {
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

	static class Or extends SubExpression {
		private Or(List<Expression> exprs) {
			this.expressions = exprs.toArray(new Expression[exprs.size()]);
		}

		public boolean eval(Map<String,Object> map) {
			for (Expression e : expressions) {
				if (e.eval(map))
					return true;
			}
			return false;
		}

		static Expression make(List<Expression> exprs) {

			for (Iterator<Expression> i = exprs.iterator(); i.hasNext();) {
				Expression e = i.next();
				if (e == TRUE)
					return TRUE;
				if (e == FALSE)
					i.remove();
			}
			if (exprs.size() == 0)
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
		Expression	expr;

		private Not(Expression expr) {
			this.expr = expr;
		}

		public boolean eval(Map<String,Object> map) {
			return !expr.eval(map);
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
		final Pattern	pattern;

		public PatternExpression(String key, String value) {
			super(key, Op.EQUAL, value);

			value = Pattern.quote(value);
			this.pattern = Pattern.compile(value.replace("\\*", ".*"));
		}

		protected boolean eval(Object scalar) {
			if (scalar instanceof String)
				return pattern.matcher((String) scalar).matches();
			else
				return false;
		}

	}

	public static class ApproximateExpression extends SimpleExpression {
		public ApproximateExpression(String key, String value) {
			super(key, Op.EQUAL, value);
		}

		protected boolean eval(Object scalar) {
			if (scalar instanceof String) {
				return ((String) scalar).trim().equalsIgnoreCase(value);
			} else
				return false;
		}
	}

	static class Rover {
		String	s;
		int		n	= 0;

		char next() {
			return s.charAt(n++);
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
			while (true) {
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

		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(s).append("\n");
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
			return s.substring(n, this.n).trim();
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

	public Expression parse(String s) throws IOException {
		Rover rover = new Rover();
		rover.s = s;
		rover.n = 0;
		return parse(rover);
	}

	public Expression parse(Requirement req) throws IOException {
		String f = req.getDirectives().get("filter");
		if (f == null)
			return new Expression() {

				@Override
				public boolean eval(Map<String,Object> map) {
					return false;
				}

				@Override
				void toString(StringBuilder sb) {}
			};

		return parse(f);
	}

	public Expression parse(Rover rover) throws IOException {
		String s = rover.findExpr();
		Expression e = cache.get(s);
		if (e != null) {
			rover.n += s.length();
			return e;
		}

		try {
			char c = rover.next();
			if (c != '(')
				throw new IllegalArgumentException("Expression must start with a (");

			e = parse0(rover);
			c = rover.next();

			if (c != ')')
				throw new IllegalArgumentException("Expression must end with a )");
		}
		catch (IllegalArgumentException ie) {
			throw new RuntimeException("Parsing failed on " + s + " " + ie.getMessage());
		}
		cache.put(s, e);
		return e;
	}

	Expression parse0(Rover rover) throws IOException {
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
					throw new IllegalArgumentException("Expected an = after " + rover.current());

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

	private List<Expression> parseExprs(Rover rover) throws IOException {
		ArrayList<Expression> exprs = new ArrayList<Expression>();
		while (rover.current() == '(') {
			Expression expr = parse(rover);
			exprs.add(expr);
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
				sb.append(namespaceToCategory(category)).append(": ");

			FilterParser fp = new FilterParser();
			String filter = r.getDirectives().get("filter");
			if (filter == null)
				sb.append("<no filter>");
			else {
				Expression parse = fp.parse(filter);
				sb.append(parse);
			}
			return sb.toString();
		}
		catch (Exception e) {
			return e.toString();
		}
	}

	public String simple(Resource resource) {
		if (resource == null)
			return "<>";

		List<Capability> capabilities = resource.getCapabilities("osgi.identity");
		if (capabilities == null || capabilities.size() == 0)
			return resource.toString();

		Capability c = capabilities.get(0);
		String bsn = (String) c.getAttributes().get("osgi.identity");
		Object version = c.getAttributes().get("version");
		if (version == null)
			return bsn;
		else
			return bsn + ";version=" + version;
	}
}
