@echo off

set CURRENTDIR=%~dp0

"%CURRENTDIR%bin\java" --module-path "%CURRENTDIR%lib" -m terms/com.maxprograms.terms.BilingualExtraction %*
