package aQute.libg;

public class Monitor<T> {
    T      value;
    final String name;
    
    public Monitor(String name, T value) {
        this.name = name;
        this.value = value;
    }
    
    public T get() {
        return value;
    }

    public synchronized void set(T t) {
        this.value = t;
    }

    public void reset() {
    }
    
    public void subscribe(MonitorListener<T> ml) {
        
    }
}
