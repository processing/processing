@echo off
jikes *.java
jexegen /nologo /out:maclf.exe /main:MacLF MacLF.class
