#!/bin/bash
adate=$(date +"%H-%M-%S")
printf " %s \n %s \n %s " "$USER" "$(hostname)" "$(date)" > "out-${adate}.txt"
