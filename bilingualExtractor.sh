#!/bin/sh

CURRENTDIR=$(dirname "$0")

"$CURRENTDIR/bin/java" --module-path "$CURRENTDIR/lib" -m terms/com.maxprograms.terms.BilingualExtraction "$@"
