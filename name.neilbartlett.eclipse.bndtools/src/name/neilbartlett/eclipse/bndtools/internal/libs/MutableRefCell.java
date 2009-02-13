package name.neilbartlett.eclipse.bndtools.internal.libs;

// @ThreadSafe
public class MutableRefCell<T> implements RefCell<T> {
	private T value;
	
	public MutableRefCell() {
		this(null);
	}

	public MutableRefCell(T value) {
		this.value = value;
	}
	
	public synchronized void setValue(T value) {
		this.value = value;
	}
	
	public synchronized T getValue() {
		return value;
	}
	
}
