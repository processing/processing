#!/bin/sh
# -Dcom.apple.hwaccel=false 
cd work && java -cp lib:lib/build:lib/pde.jar:lib/kjc.jar:lib/oro.jar:../comm.jar PdeBase
