package aQute.bnd.cdi;

import java.util.Arrays;
import java.util.stream.Collectors;

import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Instruction;

class PackageDef {
	Instruction marked;

	public PackageDef(Clazz packageInfoClazz) {
		if (packageInfoClazz == null) {
			marked = null;
			return;
		}

		marked = packageInfoClazz.annotations("org/osgi/service/cdi/annotations/Beans")
			.findFirst()
			.map(annotation -> {
				Object[] beanClasses = annotation.get("value");
				if (beanClasses != null && beanClasses.length > 0) {
					return new Instruction(Arrays.stream(beanClasses)
						.map(Object::toString)
						.collect(Collectors.joining(",")));
				}
				return new Instruction("*");
			})
			.orElse(null);
	}
}
