#!/bin/sh
clear
mvn -e clean package
echo -- ready to run --
java -jar target/flb-chatops.jar
