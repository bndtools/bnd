package biz.aQute.bnd.proxy.generator;

import java.net.NetworkInterface;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class TestFacade extends TestBase {
	public interface Delegate extends FacadeInterface {
		void bar();

		<T, E extends T> List<T> generics(Class<?> arg0, Set<? super E> arg1) throws Exception;

	}

	public static class Facade extends FacadeClass implements FacadeInterface {
		final Supplier<Delegate> bind;

		@SuppressWarnings({
			"unchecked", "rawtypes"
		})
		Facade(Function<Object, Supplier<Object>> binding) {
			this.bind = (Supplier) binding.apply(this) /* I know */;
		}

		@Override
		public void bar() {
			bind.get()
				.bar();

		}

		@Override
		public <T, E extends T> List<T> generics(Class<?> arg0, Set<? super E> arg1) throws Exception {
			return bind.get()
				.generics(arg0, arg1);

		}

		@Override
		public void foo() {
			bind.get()
				.foo();

		}

		@Override
		public int fooint() {
			return bind.get()
				.fooint();

		}

		@Override
		public String fooString() {
			return bind.get()
				.fooString();

		}

		@Override
		public void foo(String arg0, NetworkInterface arg1) {
			bind.get()
				.foo(arg0, arg1);

		}

		@Override
		public void clean(Map<String, String> args) throws Exception {
			// TODO Auto-generated method stub

		}

	}

	public Facade createFacade(Function<Object, Supplier<Object>> binder) {
		return new Facade(binder);
	}
}

