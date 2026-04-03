package bndtools.util.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import bndtools.util.ui.UI.Access;
import bndtools.util.ui.UI.Target;

class UITest {

	@SuppressWarnings("rawtypes")
	@Test
	void test() throws Exception {
		class M {
			boolean vm;
		}
		class W {
			int								n		= 100;
			final List<Consumer<Boolean>>	subs	= new ArrayList<>();
			boolean							vw;

			void set(boolean v) {
				vw = v;
				subs.forEach(x -> x.accept(v));
				n++;
			}

			AutoCloseable subscribe(Consumer<Boolean> sub) {
				subs.add(sub);
				return () -> subs.remove(sub);
			}
		}
		M model = new M();
		W world_1 = new W();
		W world_2 = new W();

		try (UI<M> ui = new UI<>(model) {
			public void dispatch() {
				update();
			}
		}) {
			Target<Boolean> target_1 = new Target<>() {

				@Override
				public void set(Boolean value) {
					world_1.set(value);
				}

				public AutoCloseable subscribe(Consumer<Boolean> subscription) {
					return world_1.subscribe(subscription);
				}
			};
			Target<Boolean> target_2 = new Target<>() {

				@Override
				public void set(Boolean value) {
					world_2.set(value);
				}

				public AutoCloseable subscribe(Consumer<Boolean> subscription) {
					return world_2.subscribe(subscription);
				}
			};

			ui.u("vm", model.vm)
				.bind(target_1)
				.bind(target_2);

			Access access = ui.access.get("vm");
			assertThat(access.last(0)).isNull();

			System.out.println("write to model true, check world update");
			ui.write(() -> model.vm = true)
				.await();
			assertThat(access.last(0)).isEqualTo(Boolean.TRUE);
			assertThat(access.last(1)).isEqualTo(Boolean.TRUE);
			assertThat(world_1.vw).isTrue();
			assertThat(world_1.n).isEqualTo(101);
			assertThat(world_2.vw).isTrue();
			assertThat(world_2.n).isEqualTo(101);

			System.out.println("write to model false, check world update");
			ui.write(() -> model.vm = false)
				.await();
			assertThat(access.last(0)).isEqualTo(Boolean.FALSE);
			assertThat(access.last(1)).isEqualTo(Boolean.FALSE);
			assertThat(world_1.vw).isFalse();
			assertThat(world_1.n).isEqualTo(102);
			assertThat(world_2.vw).isFalse();
			assertThat(world_2.n).isEqualTo(102);

			System.out.println("write to model false, check no world");
			ui.write(() -> model.vm = false)
				.await();
			assertThat(access.last(0)).isEqualTo(Boolean.FALSE);
			assertThat(access.last(1)).isEqualTo(Boolean.FALSE);
			assertThat(world_1.vw).isFalse();
			assertThat(world_1.n).isEqualTo(102);
			assertThat(world_2.vw).isFalse();
			assertThat(world_2.n).isEqualTo(102);
			assertThat(model.vm).isFalse();

			System.out.println("write to world 1 true, check other world 2 update");
			assertThat(model.vm).isFalse();
			int v = ui.lock.version;
			world_1.set(true);
			assertThat(model.vm).isTrue();
			CountDownLatch cd = ui.lock.updated;
			assertThat(cd).isNotNull();
			assertThat(world_1.vw).isTrue();
			assertThat(world_1.n).isEqualTo(103);
			assertThat(world_2.vw).isFalse();
			assertThat(world_2.n).isEqualTo(102);
			assertThat(access.last(0)).isEqualTo(Boolean.TRUE);
			assertThat(access.last(1)).isEqualTo(Boolean.FALSE);
			cd.await();

			assertThat(world_1.vw).isTrue();
			assertThat(world_1.n).isEqualTo(103);
			assertThat(world_2.vw).isTrue();
			assertThat(world_2.n).isEqualTo(103);
			assertThat(access.last(0)).isEqualTo(Boolean.TRUE);
			assertThat(access.last(1)).isEqualTo(Boolean.TRUE);
			assertThat(model.vm).isTrue();

		}

	}

	@SuppressWarnings("rawtypes")
	@Test
	void testMethodField() throws Exception {
		class M {
			int		n	= 100;
			boolean	vm;

			@SuppressWarnings("unused")
			void vm(boolean value) {
				vm = value;
				n++;
			}
		}
		M model = new M();

		try (UI<M> ui = new UI<>(model) {
			public void dispatch() {
				update();
			}
		}) {
			AtomicBoolean world = new AtomicBoolean(false);
			ui.u("vm", model.vm)
				.bind(world::set);

			Access access = ui.access.get("vm");
			access.toModel(true);
			assertThat(model.n).isEqualTo(101);
		}

	}

	@SuppressWarnings("rawtypes")
	@Test
	void testMapping() throws Exception {
		class M {
			boolean vm;
		}
		M model = new M();
		class W implements Target<String> {
			final List<Consumer<String>>	subs	= new ArrayList<>();
			String							vw;

			@Override
			public void set(String value) {
				vw = value;
				subs.forEach(c -> c.accept(value));
			}

			@Override
			public AutoCloseable subscribe(Consumer<String> subscription) {
				subs.add(subscription);
				return () -> subs.remove(subscription);
			}
		}
		W world = new W();

		try (UI<M> ui = new UI<>(model) {
			public void dispatch() {
				update();
			}
		}) {

			ui.u("vm", model.vm)
				.bind(world.map(b -> Boolean.toString(b), Boolean::valueOf));

			ui.write(() -> model.vm = true)
				.await();
			assertThat(world.vw).isEqualTo("true");

			ui.write(() -> model.vm = false)
				.await();
			assertThat(world.vw).isEqualTo("false");

			world.set("true");
			ui.lock.updated.await();
			assertThat(model.vm).isEqualTo(true);

			world.set("false");
			ui.lock.updated.await();
			assertThat(model.vm).isEqualTo(false);
		}

	}
}
