package aQute.libg;

public class Counter extends Monitor<Long>{
    long max;
    
    public Counter(String name, long value, long maxValue) {
        super(name, value);
        this.max = maxValue;
    }
    public synchronized void add( long n) {
        long r  = get() + n;
        if ( r < max && r >= 0)
            set( get() + n);
    }
    
    public void reset() {
        set(0L);
    }
    
    public void increment() {
        add(1);
    }
    
    public void decrement() {
        add(-1);
    }
}
