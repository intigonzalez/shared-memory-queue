#!/bin/bash
START=1
END=$1
echo "" > log.txt
for (( i=$START; i<=$END; i++ )) 
do
	(java MainNetPipe c -shared -consumers $1) &
done
