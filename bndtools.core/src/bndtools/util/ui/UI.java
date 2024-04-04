package bndtools.util.ui;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;

/**
 * A Utility for MVC like programming in Java.
 * <p>
 * The model is a DTO class with fields and methods. The idea is that you can
 * change these variables and then they are automatically updating the Widgets.
 * The model can contain fields and methods. This class uses a method in
 * preference of a field. A get method is applicable if the it takes the
 * identical return type as the field and has no parameters. A set method is if
 * it takes 1 parameter with the exact type of the field. A field, however, is
 * mandatory because that is how the fields are discovered. A field must be
 * non-final, non-static, non-synthetic and non-transient.
 * <p>
 * The only requirement is that you modify them in a {@link #read(Supplier)} or
 * {@link #write(Runnable)} block. These methods ensure that any updates are
 * handled thread safe and properly synchronized.
 * <p>
 * A UI is created as follows:
 *
 * <pre>
 * final M model = new M();
 * final UI<M> ui = new UI<>(model);
 * </pre>
 * <p>
 * To other side of the model is the _world_. These are widgets or methods
 * updating some information on the GUI. These are bound through a Target<T>
 * interface. The mandatory method {@link Target#set(Object)} sets the value
 * from the model to the world. The optional {@link Target#subscribe(Consumer)}
 * method can be used to let the world update the model from a subscription
 * model like addXXXListeners in SWT. There are convenient methods in this class
 * to transform common widgets to Target<X>.
 *
 * <pre>
 * ui.u("name", model.name)
 * 	.bind(UI.checkbox(myCheckbox));
 * </pre>
 * <p>
 * However, a Target<X> is also a functional interface. This makes it possible
 * to just use a lambda:
 *
 * <pre>
 * ui.u("name", model.name)
 * 	.bind(this::setTitle);
 * </pre>
 * <p>
 * The updating of the world is delayed and changes are coalesced. On the world
 * side, there is a guarantee that only changes are updated. If the subscription
 * sets a value than that value is is assumed to be the world's value. I.e. if
 * the model tries to set that same value back, the world will not be updated.
 * <p>
 * Values in the model must be simple type. Changes are detected with the normal
 * equals and hashCode. null is properly handled everywhere.
 * <p>
 * If the model requires some calculation before the world is updated, it can
 * implement Runnable. This runnable is called inside the lock to do for example
 * validation.
 *
 * @param <M> model type
 */
public class UI<M> implements AutoCloseable {
	final static Logger				log			= LoggerFactory.getLogger(UI.class);
	final static Lookup				lookup		= MethodHandles.lookup();
	final ScheduledExecutorService	scheduler	= Executors.newSingleThreadScheduledExecutor();
	final Map<String, Access>		access		= new HashMap<>();
	final List<Binder<?>>			updaters	= new ArrayList<>();
	final Class<M>					modelType;
	final List<Runnable>			updates		= new CopyOnWriteArrayList<>();
	final M							model;

	class Guarded {
		int				version	= 100;
		CountDownLatch	updated	= null;
	}

	final Guarded lock = new Guarded();

	/*
	 * The Access class maps to a single field in the model. It methods the
	 * MethodHandles to access the field or methods and it has a map of bindings
	 * and their last updated value.
	 */
	class Access implements AutoCloseable {

		final MethodHandle		get;
		final MethodHandle		set;
		final List<Binding<?>>	bindings	= new ArrayList<>();
		final Class<?>			type;
		final String			name;

		/*
		 * A Binding connects the access class to n worlds that depend on the
		 * the same value of the model. It keeps a last value and it maintains
		 * the subscription.
		 */
		class Binding<T> implements AutoCloseable {
			final Target<T>	target;
			Object			lastValue;
			AutoCloseable	subscription;

			Binding(Target<T> target) {
				this.target = target;
				subscription = target.subscribe(value -> {
					lastValue = value;
					toModel(value);
				});

			}

			@SuppressWarnings("unchecked")
			void update(Object value) {
				if (!Objects.equals(value, lastValue)) {
					lastValue = value;
					target.set((T) value);
				}
			}

			@Override
			public void close() {
				IO.close(subscription);
			}
		}

		Access(Field field) {
			this.name = field.getName();
			this.type = field.getType();
			MethodHandle get = null;
			MethodHandle set = null;
			field.setAccessible(true);
			try {
				Method m = modelType.getDeclaredMethod(name);
				m.setAccessible(true);
				get = lookup.unreflect(m);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
				try {
					get = lookup.unreflectGetter(field);
				} catch (IllegalAccessException e1) {}
			}
			try {
				Method m = modelType.getDeclaredMethod(name, type);
				m.setAccessible(true);
				set = lookup.unreflect(m);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
				try {
					set = lookup.unreflectSetter(field);
				} catch (IllegalAccessException e1) {}
			}
			assert get != null && set != null;
			this.set = set;
			this.get = get;
		}

		Object fromModel() {
			try {
				return get.invoke(model);
			} catch (Throwable e) {
				throw Exceptions.duck(e);
			}
		}

		void toModel(Object newer) {
			try {
				set.invoke(model, newer);
				trigger();
			} catch (Throwable e) {
				throw Exceptions.duck(e);
			}
		}

		@SuppressWarnings({
			"unchecked", "rawtypes"
		})
		void toWorld() {
			Object value = fromModel();
			for (Binding<?> binding : bindings) {
				binding.update(value);
			}
		}

		void add(Target<?> target) {
			bindings.add(new Binding<>(target));
		}

		@Override
		public void close() throws Exception {
			bindings.forEach(IO::close);
		}

		// test methods

		@SuppressWarnings("resource")
		Object last(int i) {
			return bindings.get(i).lastValue;
		}

		@SuppressWarnings("resource")
		Target<?> target(int i) {
			return bindings.get(i).target;
		}
	}

	/**
	 * An interface that should be implemented by parties that want to get
	 * updated and can be subscribed to. It is for this UI class the abstraction
	 * of the world.
	 * <p>
	 * Although the interface has two methods, the subscribe is default
	 * implemented as a noop. This makes this interface easy to use as a
	 * Functional interface and Consumer<T> like lambdas map well to it.
	 *
	 * @param <T> the type of the target
	 */
	public interface Target<T> {
		/**
		 * Set the model value into the world.
		 *
		 * @param value the value
		 */
		void set(T value);

		/**
		 * Subscribe to changes in the world.
		 *
		 * @param subscription the callback to call when the world changes
		 * @return a closeable that will remove the subscription
		 */
		default AutoCloseable subscribe(Consumer<T> subscription) {
			return () -> {};
		}

		/**
		 * Subscribe to changes in the world.
		 *
		 * @param subscription the callback to call when the world changes
		 * @return a closeable that will remove the subscription
		 */
		default AutoCloseable subscribe(Runnable subscription) {
			return subscribe(x -> subscription.run());
		}

		/**
		 * Sometimes the target takes a different type than the model. This
		 * method will create a mediator that maps the value back and forth.
		 *
		 * @param <U> the other type
		 * @param down the downstream towards the world
		 * @param up upstream towards the model
		 * @return another target
		 */
		default <U> Target<U> map(Function<U, T> down, Function<T, U> up) {
			Target<T> THIS = this;
			return new Target<>() {

				@Override
				public void set(U value) {
					THIS.set(down.apply(value));
				}

				@Override
				public AutoCloseable subscribe(Consumer<U> subscription) {
					AutoCloseable subscribed = THIS.subscribe(v -> {
						U apply = up.apply(v);
						subscription.accept(apply);
					});
					return subscribed;
				}
			};
		}

	}

	/**
	 * External interface to bind targets to the model.
	 *
	 * @param <T> the type
	 */
	public interface Binder<T> {
		Binder<T> bind(Target<T> target);
	}

	/**
	 * Constructor.
	 *
	 * @param model the model to use
	 */
	@SuppressWarnings("unchecked")
	public UI(M model) {
		this((Class<M>) model.getClass(), model);
	}

	/**
	 * Specify a type to use
	 *
	 * @param modelType the model type
	 * @param model the model
	 */
	UI(Class<M> modelType, M model) {
		this.modelType = modelType;
		this.model = model;

		for (Field field : modelType.getDeclaredFields()) {
			int mods = field.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || Modifier.isPrivate(mods)
				|| (field.getModifiers() & 0x00001000) != 0)
				continue;

			access.put(field.getName(), new Access(field));
		}
	}

	/**
	 * Create a binder for a given model field.
	 *
	 * @param <T> the type of the field
	 * @param name the name of the field
	 * @param guard guard to ensure the model field's type matches the targets.
	 *            The value is discarded.
	 * @return a binder
	 */
	public <T> Binder<T> u(String name, T guard) {
		assert name != null;
		Access access = this.access.get(name);
		assert access != null : name + " is not a field in the model " + modelType.getSimpleName();

		return new Binder<>() {
			@Override
			public Binder<T> bind(Target<T> target) {
				access.add(target);
				return this;
			}
		};
	}

	/**
	 * Bind the given target and return a binder for subsequent targets to bind.
	 *
	 * @param <T> the model field's type
	 * @param name the name of the field
	 * @param guard guard to ensure the model field's type matches the targets.
	 *            The value is discarded.
	 * @param target the target to bind
	 * @return a Binder
	 */
	public <T> Binder<T> u(String name, T guard, Target<T> target) {
		return u(name, guard).bind(target);
	}

	/**
	 * Return a target for a Text widget. This will use
	 * {@link Text#setText(String)} for {@link Target#set(Object)} and it will
	 * subscribe to modifications with
	 * {@link Text#addModifyListener(ModifyListener)}
	 *
	 * @param widget the text widget
	 * @return a target
	 */
	public static Target<String> text(Text widget) {
		return new Target<String>() {
			String last;

			@Override
			public void set(String value) {
				if (!Objects.equals(widget.getText(), value)) {
					System.out.println("setting " + widget + " " + value);
					last = value;
					widget.setText(value);
				}
			}

			@Override
			public AutoCloseable subscribe(Consumer<String> subscription) {
				ModifyListener listener = e -> {
					String value = widget.getText();
					if (!Objects.equals(last, value)) {
						last = value;
						System.out.println("event " + widget + " " + widget.getText());
						subscription.accept(widget.getText());
					}
				};
				widget.addModifyListener(listener);
				return () -> widget.removeModifyListener(listener);
			}
		};
	}

	/**
	 * Return a target for a checkbox button. The {@link Target#set(Object)}
	 * maps to {@link Button#setSelection(boolean)} and the subscription is
	 * handled via {@link Button#addSelectionListener(SelectionListener)}.
	 *
	 * @param widget the widget to map
	 * @return a target that can set and subscribe the button selection
	 */
	public static Target<Boolean> checkbox(Button widget) {
		return new Target<Boolean>() {

			@Override
			public void set(Boolean value) {
				widget.setSelection(value);
			}

			@Override
			public AutoCloseable subscribe(Consumer<Boolean> subscription) {
				SelectionListener listener = onSelect(e -> subscription.accept(widget.getSelection()));
				widget.addSelectionListener(listener);
				return () -> widget.removeSelectionListener(listener);
			}
		};
	}

	/**
	 * Map the selection of a CheckboxTableViewer to a Target. It uses
	 * {@link CheckboxTableViewer#setCheckedElements(Object[])} and the
	 * subscription is handled via the
	 * {@link CheckboxTableViewer#addSelectionChangedListener(ISelectionChangedListener)}
	 *
	 * @param widget the CheckboxTableViewer
	 * @return a new Target
	 */
	public static Target<Object[]> widget(CheckboxTableViewer widget) {
		return new Target<>() {

			@Override
			public void set(Object[] value) {
				widget.setCheckedElements(value);
			}

			@Override
			public AutoCloseable subscribe(Consumer<Object[]> subscription) {
				ISelectionChangedListener listener = se -> {
					subscription.accept(widget.getCheckedElements());
				};
				widget.addSelectionChangedListener(listener);
				return () -> widget.removeSelectionChangedListener(listener);
			}
		};
	}

	/**
	 * Create a selection listener with a lambda for the selection and the
	 * default selection
	 *
	 * @param listener the listener
	 * @param defaultListener the listener to default
	 * @return a proper listener
	 */
	public static SelectionListener onSelect(Consumer<SelectionEvent> listener,
		Consumer<SelectionEvent> defaultListener) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listener.accept(e);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				defaultListener.accept(e);
			}
		};
	}

	/**
	 * Create a selection listener with the same lambda for the selection and
	 * the default selection
	 *
	 * @param listener the listener
	 * @return a proper listener
	 */
	public static SelectionListener onSelect(Consumer<SelectionEvent> listener) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listener.accept(e);
			}
		};
	}

	@Override
	public void close() throws Exception {

	}

	/**
	 * Updates to the model should be handled in here. The runnable should be
	 * very short since it runs in a lock that is acquired in the display
	 * thread.
	 *
	 * @param r the runnable to execute that updates the model
	 * @return a CountDownLatch that will unlatch when the write has updated the
	 *         world.
	 */
	public CountDownLatch write(Runnable r) {
		synchronized (lock) {
			r.run();
			return trigger();
		}
	}

	/**
	 * A read in the same lock as the write but without the world update. The
	 * supplier's value is returned.
	 *
	 * @param <X> the type the return
	 * @param r the supplier
	 * @return the value returned from the supllier
	 */
	public <X> X read(Supplier<X> r) {
		synchronized (lock) {
			return r.get();
		}
	}

	/**
	 * Trigger a world update. This will delay 50 ms to coalesce additional
	 * updates. If during the update of the world (which is done without holding
	 * a lock) there is another change, the update will be repeated.
	 *
	 * @return a {@link CountDownLatch} that will be unlatched when the state of
	 *         the model at this moment is represented in the world.
	 */
	public CountDownLatch trigger() {
		synchronized (lock) {
			lock.version++;
			if (lock.updated == null) {
				lock.updated = new CountDownLatch(1);
				scheduler.schedule(() -> {
					while (true) {
						int current;
						synchronized (lock) {
							current = lock.version;
						}

						try {
							dispatch();
						} catch (Exception e) {
							log.error("failed to update model to world {}", e, e);
						}

						synchronized (lock) {
							if (current == lock.version) {
								lock.updated.countDown();
								lock.updated = null;
								return;
							}
						}
					}
				}, 50, TimeUnit.MILLISECONDS);
			}
			return lock.updated;
		}
	}

	/**
	 * This method is Eclispe SWT specific. It dispatches the updates on the UI
	 * thread when there are no events present.
	 */
	void dispatch() {
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			while (!display.isDisposed()) {
				if (!display.readAndDispatch()) {
					update();
					return;
				}
			}
		});
	}

	/**
	 * Copy the model to the world.
	 */
	public void update() {
		if (model instanceof Runnable r) {
			r.run();
		}

		for (Access access : this.access.values()) {
			access.toWorld();
		}
	}
}
