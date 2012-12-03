#!/bin/bash

if [ "$JAVA_HOME" == "" ]; then
    echo "Please specify JAVA_HOME to point to JDK8 image"
    exit 1
fi

if [ -f "$JAVA_HOME/bin/sparcv9/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/sparcv9/java"
else
    JAVA_BIN="$JAVA_HOME/bin/java"
fi

if [ "$CP" == "" ]; then
    CP=../out/production/test
fi

if [ "$BCP" == "" ]; then
    BCP=../out/production/jdk
fi

OPTS="-Xmx4G -cp $CP"

if [ "$1" == "s" ]; then
  shift;
  echo "Skipping non-patched run"
else

echo ""
echo "Executing: $JAVA_BIN $OPTS $* reference"
echo ""
$JAVA_BIN $OPTS $* reference

fi

OPTS="$OPTS -Xbootclasspath/p:$BCP"

echo ""
echo "Executing: $JAVA_BIN $OPTS $*"
echo ""
$JAVA_BIN $OPTS $*
