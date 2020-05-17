#!/bin/sh

export CLASSPATH=$CLASSPATH:./lib/postgresql-42.2.12.jar:./asg.cliche/src:.:../../../lib/postgresql-42.2.12.jar:../../../asg.cliche/src

# Cliche Compile:
clear && clear
javac ./asg.cliche/src/asg/cliche/util/*.java
javac ./asg.cliche/src/asg/cliche/*.java

# GradeBookShell Compile:
javac ./src/main/java/edu/boisestate/cs410/gradebook/GradeBookShell.java

# Execute:
cd ./src/main/java
java edu.boisestate.cs410.gradebook.GradeBookShell 'postgresql://localhost:3XXXX/database-name?user=user-name&password=your-password-here'

#

