package bndtools.editor.model.conversions;

public interface Converter<R, T> {
    R convert(T input) throws IllegalArgumentException;
}