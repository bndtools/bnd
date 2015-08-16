package bndtools.views.repository;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class ClassLockSchedulingRule implements ISchedulingRule {

    private final Class< ? > clazz;

    public ClassLockSchedulingRule(Class< ? > clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean contains(ISchedulingRule other) {
        return false;
    }

    @Override
    public boolean isConflicting(ISchedulingRule other) {
        if (other instanceof ClassLockSchedulingRule) {
            Class< ? > otherClazz = ((ClassLockSchedulingRule) other).clazz;
            return clazz.isAssignableFrom(otherClazz) || otherClazz.isAssignableFrom(clazz);
        }
        return false;
    }

}
