getPrev() # get previous significant word, i.e. not an option flag
{
	case "${COMP_WORDS[COMP_CWORD-$1]}" in
	    -*)
		getPrev  $(($1+1))
		;;
	    *)
		echo "${COMP_WORDS[COMP_CWORD-$1]}"
		;;
	esac
}

_bnd_completion_()
{
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    [[ ${cur} == -* ]] && return 0 # Not even trying to complete options

    # Basic commands
    opts="%listCommands%"

    prev=$(getPrev 1)
    
    #
    # Argument completion
    #	
    case "${prev}" in
	bnd|help)
	    COMPREPLY=($(compgen -W "${opts}" $cur))
	    return 0
	    ;; 
	*)
	    COMPREPLY=( $(compgen -f ${cur}) ) # List of files by default
        return 0
        ;;	
    esac

}

complete -F _bnd_completion_ bnd
