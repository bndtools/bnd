---
layout: default
class: Macro
title: if ';' STRING ';' STRING ( ';' STRING )?
summary: Conditional macro that depending on a condition returns either a value for true or optionally for false.
---

	if         ::= 'if' ';' condition ';' expansion ( ';' expansion )?
	condition  ::= true | false
	false      ::= 'false' | ''
	true       ::= ! false
    expansion  ::= ...
    	
The `${if}` macro allows a conditional expansion. The first argument is the _condition_. the condition is either _false_ (empty or 'false') or otherwise _true_. If the condition is true, the value of the macro is the second argument, the first _expansion_. Otherwise, if a third argument is specified this is returned. If no third argument is specified, an empty string is  returned.

Notice that if a macro is not set, it will revert to its literal value. This can be confusing. For example, if the macro `foo` is **not** set, the `${if;${foo};TRUE;FALSE}` will be `TRUE`. If you want to handle unset as false, the [def](def.html) might be of use.

## Examples

    # expands to 'B'
	aorb = ${if;;A;B}
	
	# Display ${foo} if set to a non-empty string that is not false, otherwise 'Ouch'. See also ${def} 
	whatisfoo = ${if;${foo};${foo};Ouch}
	
	# Include a file conditionally. If ${test} is not empty or false, the file is included
	-include ${if;${test};test.bnd}
