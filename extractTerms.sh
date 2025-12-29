#!/bin/bash
export CURRENT=$PWD
cd `dirname "$0"`
export TERMS_HOME=$PWD
cd $CURRENT
$TERMS_HOME/bin/java --module-path $TERMS_HOME/lib -m terms/com.maxprograms.terms.TermExtractor $@
