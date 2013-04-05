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
    opts="candidates certificate command deinit deposit find gc init install jpm keys \
	log platform register remove restart service settings setup start status \
	stop trace version winreg"

    prev=$(getPrev 1)
    
    #
    # Argument completion
    #	
    case "${prev}" in
	jpm)
	    COMPREPLY=($(compgen -W "${opts}" $cur))
	    return 0
	    ;;
	    
	command)
	    COMPREPLY=( $(compgen -W "$(listCommands)" -- ${cur}) )
	    return 0
	    ;;
	remove)
	    local result="$(listCommands) $(listServices)"
	    COMPREPLY=( $(compgen -W "${result}" -- ${cur}) )
	    return 0
	    ;;
	service|start|stop|restart|log)
	    COMPREPLY=( $(compgen -W "$(listServices)" -- ${cur}) )
	    return 0
	    ;;
	*)
	    COMPREPLY=( $(compgen -f ${cur}) ) # List of files by default
            return 0
            ;;	
    esac

}

#Assign the auto-completion function _get for our command get.
complete -F _jpm_completion_ jpm
