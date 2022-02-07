package bndtools.core.test.builder;

import org.assertj.core.api.SoftAssertions;
import org.bndtools.test.assertj.eclipse.jdt.core.JDTCoreSoftAssertionsProvider;
import org.bndtools.test.assertj.eclipse.resources.ResourcesSoftAssertionsProvider;

public class AllSoftAssertions extends SoftAssertions
	implements JDTCoreSoftAssertionsProvider, ResourcesSoftAssertionsProvider {

}
