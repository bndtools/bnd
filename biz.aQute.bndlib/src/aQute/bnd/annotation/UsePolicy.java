package aQute.bnd.annotation;

import java.lang.annotation.*;

/**
 * This annotation can be applied to interface where an implementation should be
 * treated as a use policy, not an implementation policy. Many package have
 * interfaces that are very stable and can be maintained backward compatible for
 * implementers during minor changes. For example, in Event Admin, the
 * EventAdmin implementers should follow the minor version, e.g. [1.1,1.2),
 * however, an implementer of EventHandler should not require such a small
 * range. Therefore an interface like EventHandler should use this anotation.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Deprecated
public @interface UsePolicy {
	String	RNAME	= "LaQute/bnd/annotation/UsePolicy;";

}
