#### MASON Makefile
#### By Sean Luke

#### Relevant Stuff:
#### To see all your make options:  type   make help
#### To switch from jikes to javac:  change the JAVAC variable below
#### To add flags (like -O) to javac:  change the FLAGS variable below

# to compile: make
# to run: java -cp .:$CLASSPATH sim.app.horde.SimHordeWithUI
# or java sim.app.horde.SimHordeWithUI
# enjoy !!!

#JAVAC = /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/javac ${JAVACFLAGS}
JAVAC = javac ${JAVACFLAGS}

JAVACFLAGS = -target 1.6 -source 1.6 ${FLAGS}
JIKESFLAGS = -target 1.6 +Pno-shadow ${FLAGS}
FLAGS = -g -nowarn

# Main java files, not including the 3D stuff
DIRS = \
*.java\
behaviors/*.java \
classifiers/decisiontree/*.java \
classifiers/decisiontree/pruning/*.java \
features/*.java \
objects/*.java \
targets/*.java \
transitions/*.java \
hardware/*.java \
scenarios/pioneer/*.java \
scenarios/pioneer/behaviors/*.java \
scenarios/pioneer/features/*.java \
scenarios/pioneer/targets/*.java \
scenarios/pioneer/vision/*.java \
irl/*.java \
optimization/cobyla/*.java \

# Make the main MASON code, not including 3D code
all:
	${JAVAC} ${DIRS}


# Delete all jmf gunk, checkpoints, backup emacs gunk classfiles,
# documentation, and odd MacOS X poops
clean:
	find . -name "*.class" -exec rm -f {} \;
	find . -name "jmf.log" -exec rm -f {} \;
	find . -name ".DS_Store" -exec rm -f {} \; 
	find . -name "*.checkpoint" -exec rm -f {} \;
	find . -name "*.java*~" -exec rm -f {} \;
	find . -name "*.swp" -exec rm -rf {} \;
	find . -name ".#*" -exec rm -rf {} \;
	find . -name "*.java.orig" -exec rm -f {} \;

# Indent to your preferred brace format using emacs.  MASON's default
# format is Whitesmiths at 4 spaces.  Yes, I know.  Idiosyncratic.
# Anyway, beware that this is quite slow.  But it works!
indent: 
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;

# A better indentation command, faster than the emacs one. Assuming that 'astyle' is installed on your machine. The command below converts all codes to Allman's style
indent-fast:
	@files=`find . -name "*.java"` ; \
	for file in $$files ; do \
		echo formatting $$file ; \
		astyle --style=allman --indent=tab $$file ; \
		# to do a whitesmith at 4 spaces, uncomment this line -- ;\
		# astyle --style=whitesmith --indent=spaces=4 $$file ; \
	done
