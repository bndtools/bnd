package org.bndtools.templating.engine.mustache;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.reflect.BaseObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * A Mustache ObjectHandler that throws an error if a template parameter is unbound.
 */
class CheckMissingObjectHandler extends BaseObjectHandler {
    private final ObjectHandler delegateHandler;

    CheckMissingObjectHandler(ObjectHandler delegateHandler) {
        this.delegateHandler = delegateHandler;
    }

    @Override
    public Wrapper find(final String name, Object[] scopes) {
        final Wrapper delegateWrapper = delegateHandler.find(name, scopes);
        return new Wrapper() {
            @Override
            public Object call(Object[] scopes) throws GuardException {
                Object value = delegateWrapper.call(scopes);
                // System.out.printf("Wrapper.call() [name=%s] -> %s%n", name, value);
                if (value == null)
                    throw new IllegalArgumentException(String.format("Missing value for %s", name));
                return value;
            }
        };
    }

    @Override
    public Binding createBinding(final String name, TemplateContext context, Code code) {
        final Binding delegateBinding = delegateHandler.createBinding(name, context, code);
        return new Binding() {
            @Override
            public Object get(Object[] scopes) {
                Object value = delegateBinding.get(scopes);
                // System.out.printf("Binding.get() [name=%s] -> %s%n", name, value);
                if (value == null)
                    throw new IllegalArgumentException(String.format("Missing value for %s", name));
                return value;
            }
        };
    }
}