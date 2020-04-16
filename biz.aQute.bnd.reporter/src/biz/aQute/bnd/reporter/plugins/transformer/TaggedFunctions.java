package biz.aQute.bnd.reporter.plugins.transformer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;

public class TaggedFunctions extends SimpleJtwigFunction {
	public static final String	TAG_DEFAULT	= "default";
	private List<Pattern>		tags;

	public TaggedFunctions() {
		this(null);
	}

	public TaggedFunctions(List<String> tags) {
		this.tags = Optional.ofNullable(tags)
			.orElse(Arrays.asList(TAG_DEFAULT))
			.parallelStream()
			.map(tag -> {
				try {
						if (tag.equals("*")) {
							tag = ".*";
						}
						return Pattern.compile(tag);
				} catch (Exception e) {
						throw new RuntimeException("Tag is not a compileable pattern", e);
				}
				})
			.collect(Collectors.toList());
	}

	@Override
	public String name() {
		return "tagged";
	}

	@Override
	public Object execute(FunctionRequest functionRequest) {

		return functionRequest.getArguments()
			.parallelStream()
			.flatMap(o -> {
				if (o instanceof Collection) {
					return ((Collection<?>) o).stream();
					} else if (o instanceof Object[]) {
						return Stream.of(((Object[]) o));
				} else {
					return Stream.of(o);
				}
			})
			.map(Object::toString)
			.anyMatch(argument -> {
				return tags.stream()
					.map(p -> p.matcher(argument))
					.anyMatch(Matcher::find);
			});
	}

}
