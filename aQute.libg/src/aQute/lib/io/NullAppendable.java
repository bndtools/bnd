package aQute.lib.io;
public class NullAppendable implements Appendable {

    @Override
    public Appendable append(CharSequence csq) {
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        return this;
    }

    @Override
    public Appendable append(char c) {
        return this;
    }
}