@echo off
@setlocal
REM  this script does the following:
REM  1. install libraries that are not available in maven central, in local repository (Xerces with xsd 1.1, Xeger)
REM  2. create stripped JVM for packaging 
REM 
REM  7z cli, JDK with javafx and maven must be in system PATH

cd /D "%~dp0"
curl -L https://dlcdn.apache.org//xerces/j/binaries/Xerces-J-bin.2.12.2-xml-schema-1.1.zip -o Xerces.zip || goto :error

curl -L https://dlcdn.apache.org//xerces/j/source/Xerces-J-src.2.12.2-xml-schema-1.1.zip -o XercesSrcOrig.zip || goto :error 

7z e -r -aoa Xerces.zip xercesImpl.jar || goto :error

REM rename for automatic module name
copy /y xercesImpl.jar xercesimpl-xsd11.jar
REM remove org/w3c dir (java 17 conflict)
7z d xercesimpl-xsd11.jar org/w3c/

REM prepare src zip
7z x XercesSrcOrig.zip -aoa -y

pushd xerces-2_12_2-xml-schema-1.1\src || goto :error
rd /s /q org\w3c
7z a -tzip ..\..\XercesSrc.zip *
popd

call mvn install:install-file -Dfile=xercesimpl-xsd11.jar -DgroupId=org.apache.xerces -DartifactId=xercesimpl-xsd11 -Dversion=2.12.2 -Dpackaging=jar -DgeneratePom=true -Dsources=XercesSrc.zip || goto :error

REM download xeger from https://github.com/agarciadom/xeger/packages/1489962
call mvn install:install-file -Dfile=xeger-1.0.2.jar -DgroupId=nl.flotsam -DartifactId=xeger -Dversion=1.0.2 -Dpackaging=jar -DgeneratePom=true || goto :error

REM create stripped jvm for packaging
rd /s /q ..\java-runtime >nul 2>&1 
jlink --no-header-files --no-man-pages --add-modules java.base,java.prefs,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics --output ..\java-runtime || goto :error
goto :EOF

:error
echo Failed with error #%errorlevel%.
exit /b %errorlevel%
