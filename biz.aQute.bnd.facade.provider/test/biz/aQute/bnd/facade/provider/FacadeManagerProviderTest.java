package biz.aQute.bnd.facade.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import biz.aQute.bnd.facade.api.Binder;
import biz.aQute.bnd.facade.api.FacadeManager;
import biz.aQute.bnd.facade.api.JTAGBinder;
import biz.aQute.bnd.facade.api.Memento;

class FacadeManagerProviderTest {
	static JTAGBinder	testAdapter	= new JTAGBinder();
	final static String	ID			= "biz.aQute.bnd.facade.provider.FacadeManagerProviderTest.DomainImpl";

	interface Domain {
		void set(String s);

		String get();
	}

	static AtomicInteger counter = new AtomicInteger(0);

	@Component(scope = ServiceScope.PROTOTYPE, property = FacadeManager.FACADE_ID + "=" + ID)
	public static class DomainImpl implements Domain, Memento {

		volatile String state;

		public DomainImpl() {
			System.out.println("DomainImpl + " + counter.incrementAndGet());
		}

		@Override
		public void set(String s) {
			this.state = s;

		}

		@Override
		public String get() {
			return state;
		}

		@Override
		public void setState(Object state, WeakReference<?> facade) {
			this.state = (String) state;
		}

		@Override
		public Object getState() {
			return state;
		}

		@Deactivate
		void deactivate() {
			System.out.println("DomainImpl - " + counter.getAndDecrement());
		}
	}

	public static class DomainFacade implements Domain {
		final Binder<Domain> binder = Binder.create(this, Domain.class, ID, null);

		@Override
		public void set(String s) {
			binder.get().set(s);
		}

		@Override
		public String get() {
			return binder.get().get();
		}

	}

	final static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("launch.bndrun");

	@Test
	void testPrototypeScoped() throws Exception {
		try (Launchpad lp = builder.create()) {

			DomainFacade facade_1 = new DomainFacade();
			Binder<Domain> binder_1 = facade_1.binder;
			assertThat(testAdapter.reg(binder_1)).isNull();
			assertThat(testAdapter.binders()).contains(binder_1).hasSize(1);
			assertThat(testAdapter.isClosed(binder_1)).isFalse();


			assertThat(binder_1.peek()).isNull();
			assertThat(testAdapter.binders()).contains(binder_1).hasSize(1);
			assertThat(testAdapter.facadeManager()).isNull();
			assertThat(testAdapter.isClosed(binder_1)).isFalse();
			assertThat(testAdapter.reg(binder_1)).isNull();

			System.out.println("register a facade manager");

			FacadeManagerProvider fm = new FacadeManagerProvider(lp.getBundleContext());
			ServiceRegistration<FacadeManager> register = lp.register(FacadeManager.class, fm);

			System.out.println("check if fm was found by the facade");
			assertThat(testAdapter.facadeManager()).isEqualTo(fm);
			assertThat(testAdapter.reg(binder_1)).isNotNull();
			assertThat(testAdapter.binders()).contains(binder_1).hasSize(1);
			assertThat(testAdapter.isClosed(binder_1)).isFalse();
			assertThat(binder_1.peek()).isNull();

			System.out.println("start a delegate component with prototype scope");
			Bundle component = lp.component(DomainImpl.class);

			System.out.println("check if we now are bound to an instance");
			assertThat(binder_1.peek()).isNotNull().isInstanceOf(Domain.class);
			assertThat(testAdapter.isClosed(binder_1)).isFalse();

			System.out.println("check if we can transfer state between cycling the life of the delegate");
			facade_1.set("s1");
			assertThat(facade_1.get()).isEqualTo("s1");
			component.stop();

			assertThat(binder_1.peek()).isNull();
			binder_1.setTimeout(100);
			long now = System.currentTimeMillis();
			try {
				facade_1.get();
				fail("Exepected an exception after 100 ms");
			} catch( IllegalStateException e) {
				assertThat(System.currentTimeMillis()-now).isGreaterThanOrEqualTo(100);
			}

			System.out.println("start the component again, see if state was preserved");
			component.start();

			assertThat(binder_1.peek()).isNotNull().isInstanceOf(Domain.class);
			assertThat(facade_1.get()).isEqualTo("s1");
			assertThat(binder_1.getState()).isNull();

			System.out.println("Register a second facade, check no state");

			DomainFacade facade_2 = new DomainFacade();
			Binder<Domain> binder_2 = facade_2.binder;

			assertThat(binder_2.peek()).isNotNull();
			assertThat(testAdapter.reg(binder_2)).isNotNull();
			assertThat(facade_2.get()).isNull();
			assertThat(testAdapter.binders()).contains(binder_1).contains(binder_2).hasSize(2);

			System.out.println("Life cycle the FM, check no binder");
			register.unregister();
			fm.deactivate();

			assertThat(testAdapter.facadeManager()).isNull();
			assertThat(testAdapter.reg(binder_1)).isNull();
			assertThat(testAdapter.reg(binder_2)).isNull();
			assertThat(testAdapter.binders()).contains(binder_1).contains(binder_2).hasSize(2);
			assertThat(testAdapter.isClosed(binder_1)).isFalse();
			assertThat(testAdapter.isClosed(binder_2)).isFalse();
			assertThat(binder_1.peek()).isNull();
			assertThat(binder_2.peek()).isNull();

			System.out.println("Register new FM");
			fm = new FacadeManagerProvider(lp.getBundleContext());
			register = lp.register(FacadeManager.class, fm);

			assertThat(testAdapter.facadeManager()).isEqualTo(fm);
			assertThat(testAdapter.reg(binder_1)).isNotNull();
			assertThat(testAdapter.reg(binder_2)).isNotNull();
			assertThat(testAdapter.binders()).containsExactlyInAnyOrder(binder_1,binder_2);
			assertThat(binder_1.get()).isNotNull().isInstanceOf(Domain.class);
			assertThat(binder_2.get()).isNotNull().isInstanceOf(Domain.class);
			assertThat(facade_1.get()).isEqualTo("s1");
			assertThat(facade_2.get()).isNull();

			System.out.println("Check if we clean up when gc'ed");
			facade_1 = null;
			System.gc();
			Binder.purge();


			assertThat(fm.controllers.get(ID).binders.keySet()).containsExactlyInAnyOrder(binder_2);
			assertThat(testAdapter.binders()).containsExactly(binder_2);
			assertThat(testAdapter.isClosed(binder_1)).isTrue();
			assertThat(testAdapter.reg(binder_1)).isNull();

			assertThat(testAdapter.isClosed(binder_2)).isFalse();
			assertThat(binder_2.peek()).isNotNull();

			System.out.println("Test if we can close a binder");
			assertThat(testAdapter.binders()).containsExactly(binder_2);
			assertThat(testAdapter.reg(binder_2)).isNotNull();
			assertThat(fm.controllers.get(ID).binders.keySet()).containsExactly(binder_2);
			binder_2.close();
			assertThat(testAdapter.isClosed(binder_2));
			assertThat(testAdapter.reg(binder_2)).isNull();
			assertThat(testAdapter.binders()).isEmpty();
			assertThat(fm.controllers.get(ID).binders).isEmpty();

			System.out.println("unregister second FM");
			register.unregister();
			fm.deactivate();


			System.out.println("check component symmetry");
			component.stop();
			Awaitility.await().until(()->counter.get()==0);

			System.out.println("Close the framework");
		}
	}

}
