@echo off
pushd "%~dp0" 
set TERMS_HOME=%CD%
popd
%TERMS_HOME%\bin\java.exe --module-path %TERMS_HOME%\lib -m terms/com.maxprograms.terms.TermExtractor %* 