#!/usr/bin/env python
# encoding: utf-8

import sys
import os
import re

def main():	
	directory = 'api_examples'
	for root, dirs, files in os.walk(directory):
			for name in files:
				# convertTags(os.path.join(root,name), 'c', 'kbd')
				# moveFile(root,name)
				# addCDATA(os.path.join(root, name))

def moveFile(root, name):
	data = open(os.path.join(root,name),'r').read()
	include = re.compile(r'<subcategory>((Primitive)|(Composite)|(Relational Operators)|(Iteration)|(Conditionals)|(Logical Operators))').match
	if(include(data)):
		print "Moving " + os.path.join(root,name) + "to root" + "include/" + name
		os.rename(os.path.join(root,name), root + "include/" + name)
	
		
def addCDATA(f):
	if(f[-3:] == 'xml'):
		xml = open(f, 'r')
		txt = xml.read()
	
		# pattern = re.compile(r'(<description>(?!<\!\[CDATA))([\s\S]+?)(</description>)', re.MULTILINE)
		# pattern = re.compile(r'(<syntax>(?!<\!\[CDATA))([\s\S]+?)(</syntax>)', re.MULTILINE)
		pattern = re.compile(r'(<code>(?!<\!\[CDATA))([\s\S]+?)(</code>)', re.MULTILINE)
		txt = re.sub( pattern, r'\1<![CDATA[\2]]>\3', txt)
	
		# pattern = re.compile(r'(<code>(?!<\!\[CDATA))([\s\S]+?)(</code>)', re.MULTILINE)
		# 	txt = re.sub( pattern, r'\1<![CDATA[\2]]>\3', txt)
	
		xml.close()
		xml = open(f, 'w')
		xml.write(txt)
		xml.close()

def convertTags(f, pName, newName):
	xml = open(f, 'r')	
	txt = xml.read()
	
	reString = '(<'+ pName + '>)([\s\S]+?)(</' + pName + r'>)'
	pattern = re.compile( reString, re.MULTILINE)
	txt = re.sub(pattern, r'<' + newName + r'>\2</'+ newName +'>', txt)
	
	xml.close()
	xml = open(f, 'w')
	xml.write(txt)
	xml.close()

def removeCDATA(f):
	xml = open(f, 'r')
	txt = xml.read()
	
	pattern = re.compile(r'<\!\[CDATA\[', re.MULTILINE)
	txt = re.sub( pattern, r'', txt)
	
	pattern = re.compile(r'\]\]>')
	txt = re.sub(pattern, r'', txt)
	
	xml.close()
	xml = open(f, 'w')
	xml.write(txt)
	xml.close()
	print "wrote: " + f


if __name__ == '__main__':
	main()

