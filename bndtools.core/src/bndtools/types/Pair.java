package bndtools.types;

import java.io.Serializable;

public class Pair<A,B> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final A first;
    private final B second;

    public Pair(A first, B second) {
        assert first != null && second != null : "both parameters must be non-null";
        this.first = first; this.second = second;
    }

    public static <A,B> Pair<A, B> newInstance(A first, B second) {
        return new Pair<A, B>(first, second);
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "Pair [" + first + ", " + second + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        Pair<A, B> other = (Pair<A, B>) obj;
        if (first == null) {
            if (other.first != null)
                return false;
        } else if (!first.equals(other.first))
            return false;
        if (second == null) {
            if (other.second != null)
                return false;
        } else if (!second.equals(other.second))
            return false;
        return true;
    }

    @Override
    public Pair<A,B> clone() {
        return new Pair<A,B>(first, second);
    }
}