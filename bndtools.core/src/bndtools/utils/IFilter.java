package bndtools.utils;

public interface IFilter<T> {
	boolean select(T object);
}
