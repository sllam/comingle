
install:
	echo "Creating bin/comingle"
	mkdir -p bin/comingle
	for cmdir in "facts" "tuple" "logging" "misc" "hash" "goals" "store" "rewrite" ; do \
		echo "Compiling $$cmdir" ; \
		javac comingle/$$cmdir/*.java ; \
		echo "Creating bin/comingle/$$cmdir" ; \
		mkdir -p bin/comingle/$$cmdir ; \
		echo "Moving $$cmdir.class" ; \
		mv comingle/$$cmdir/*.class bin/comingle/$$cmdir/. ; \
	done
	echo "Creating comingle.jar"
	cd bin ; jar cf comingle.jar comingle

clean:
	-rm comingle/facts/*.class
	-rm comingle/goals/*.class
	-rm comingle/hash/*.class
	-rm comingle/logging/*.class
	-rm comingle/misc/*.class
	-rm comingle/rewrite/*.class
	-rm comingle/store/*.class
	-rm comingle/tuple/*.class

	-rm -r bin/comingle

	-rm bin/comingle.jar

