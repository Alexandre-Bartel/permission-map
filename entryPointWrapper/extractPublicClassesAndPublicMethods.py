#!/usr/bin/python

import sys

if __name__ == '__main__':
	fn = sys.argv[1]
	f = open(fn)

	inPublicClass = False
	classNbr = 0
	methodNbr = 0

	for l in f:
		try:
			if l.startswith("[I] Class"): # class declaration
				inPublicClass = False
				if (l.startswith("[I] Class com.android.") or l.startswith("[I] Class android.")) and l.find("public: 1") > 0:
					inPublicClass = True
					className = l.split(": ")[0].split(" ")[-1]
					#print className
					classNbr += 1
			elif inPublicClass and l.startswith("  [I] Method"): # method declaration
				methodName = l.split(")>")[0].split("<")[-1]
				#print "  "+ methodName
				methodNbr += 1
		except Exception as e:
			print e
			print l
	f.close()


	print "# classes: ", classNbr
	print "# methods: ", methodNbr
