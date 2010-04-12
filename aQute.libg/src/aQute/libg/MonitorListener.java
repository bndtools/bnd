package aQute.libg;

public interface MonitorListener<T> {
    void changed(T value);
}
