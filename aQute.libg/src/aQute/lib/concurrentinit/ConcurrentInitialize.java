package aQute.lib.concurrentinit;

/**
 * Helper class to handle concurrent system where you need to initialize a
 * value. The first one should create the value but the others should block
 * until the value has been created. Since we do not want to hold a lock during
 * the creation this is kind of tricky. This class uses a single monitor
 * {@link #lock} that oeprates a state machine.
 *
 * @param <T>
 */
public abstract class ConcurrentInitialize<T> {
	enum State {
		/*
		 * Initial state, the first one that detects the machine is in this
		 * state must create the object
		 */
		INIT,
		/*
		 * The object is being created, block until errored or created
		 */
		CREATING,
		/*
		 * There is an object, just return it
		 */
		DONE,
		/*
		 * There was an error during creation, throw the error
		 */
		ERROR
	};

	private State		state	= State.INIT;
	private T			value;
	private Object		lock	= new Object();
	private Thread		creatingThread;
	private Exception	exception;

	/**
	 * Get the value or wait until it is created.
	 */
	public T get() throws Exception {
		synchronized (lock) {
			switch (state) {
				case INIT :
					state = State.CREATING;
					creatingThread = Thread.currentThread();
					break;

				case CREATING : {
					if (creatingThread == Thread.currentThread())
						throw new IllegalStateException(
								"Cycle:  ConcurrentInitialize's create returns to same instance");
					do {
						lock.wait();
					} while (state == State.CREATING);

					if (state == State.ERROR)
						throw exception;
					return value;
				}

				case ERROR :
					throw exception;

				case DONE :
					return value;
			}
		}
		try {
			set(create(), null, State.DONE);
			return value;
		} catch (Exception e) {
			set(null, e, State.ERROR);
			throw e;
		}
	}

	private void set(T value, Exception e, State state) {
		synchronized (lock) {
			this.value = value;
			this.state = state;
			this.creatingThread = null;
			this.lock.notifyAll();
			this.exception = e;
		}
	}

	/**
	 * Override to create the actual object
	 *
	 * @return the actual object, could be null
	 * @throws Exception if the creation failed this is the exception that was
	 *             thrown
	 */
	public abstract T create() throws Exception;
}
