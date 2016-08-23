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

## Examples

    # expands to 'B'
	aorb = ${if;;A;B}
	
	# Display ${foo} if set, otherwise 'Ouch'. See also ${def} 
	whatisfoo = ${if;${foo};${foo};Ouch}
	
	# Include a file conditionally
	-include ${if;${test};test.bnd}
