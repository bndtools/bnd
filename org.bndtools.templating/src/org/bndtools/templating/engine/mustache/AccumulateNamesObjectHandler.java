package org.bndtools.templating.engine.mustache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.reflect.BaseObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * A Mustache ObjectHandler that accumulates the names of requested parameters
 */
class AccumulateNamesObjectHandler extends BaseObjectHandler {

    private final ObjectHandler delegateHandler;
    private final Set<String> names = new HashSet<>();

    AccumulateNamesObjectHandler(ObjectHandler delegateHandler) {
        this.delegateHandler = delegateHandler;
    }

    @Override
    public Wrapper find(final String name, Object[] scopes) {
        final Wrapper delegateWrapper = delegateHandler.find(name, scopes);
        return new Wrapper() {
            @Override
            public Object call(Object[] scopes) throws GuardException {
                names.add(name);
                return delegateWrapper.call(scopes);
            }
        };
    }

    @Override
    public Binding createBinding(final String name, TemplateContext context, Code code) {
        final Binding delegateBinding = delegateHandler.createBinding(name, context, code);
        return new Binding() {
            @Override
            public Object get(Object[] scopes) {
                names.add(name);
                return delegateBinding.get(scopes);
            }
        };
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(names);
    }

}