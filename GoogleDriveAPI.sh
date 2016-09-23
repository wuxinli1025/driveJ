#!/bin/bash
homeAPI=/Users/Ryan7WU/driveAPI/
pwd=`pwd`/

PUT() {
	cd "$homeAPI"
	ln -s "$pwd$1" "$homeAPI$1"
	gradle -q run -Parg="$1"
	rm "$1"
}

if [[ $# -eq 0 ]]; then
	cd "$homeAPI"
	gradle -q run
fi

if [[ $# -eq 1 ]]; then
	case $1 in
		-r ) cd "$homeAPI"; gradle -q run -Parg="remove"
			;;
		* ) PUT "$1"
	esac
fi

if [[ $# > 1 ]]; then
	for arg in "$@"; do
		PUT "$arg"
	done
fi

cd "$pwd"
