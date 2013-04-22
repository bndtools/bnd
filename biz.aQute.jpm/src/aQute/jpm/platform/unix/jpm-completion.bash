listCommands()
{
    echo "$(jpm command | cut -d\  -f 1)"
}

listServices()
{
    echo "$(jpm service | cut -d\  -f 1)"
}

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

_jpm_completion_()
{
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    [[ ${cur} == -* ]] && return 0 # Not even trying to complete options

    # Basic commands
    opts="%listJpmCommands%"

    prev=$(getPrev 1)
    
    #
    # Argument completion
    #	
    case "${prev}" in
	jpm|help)
	    COMPREPLY=($(compgen -W "${opts}" $cur))
	    return 0
	    ;; 
	command)
	    COMPREPLY=( $(compgen -W "$(listCommands)" -- ${cur}) )
	    return 0
	    ;;
	remove|update)
	    local result="$(listCommands) $(listServices)"
	    COMPREPLY=( $(compgen -W "${result}" -- ${cur}) )
	    return 0
	    ;;
	service|start|stop|restart|log)
	    COMPREPLY=( $(compgen -W "$(listServices)" -- ${cur}) )
	    return 0
	    ;;
	setup)
		COMPREPLY=( $(compgen -W "local global" -- ${cur}) )
		return 0
		;;
	generate)
		COMPREPLY=( $(compgen -W "markdown bash-completion" -- ${cur}) )
		return 0
		;;  
	*)
	    COMPREPLY=( $(compgen -f ${cur}) ) # List of files by default
            return 0
            ;;	
    esac

}

complete -F _jpm_completion_ jpm
