#!/bin/sh

find src -name "*.java" -exec java -jar google-java-format-1.23.0-all-deps.jar --replace {} \;
