
attachDB:
	for pid in `adb jdwp` ; do \
		echo "Connecting to Pid:$$pid" ; \
		adb forward tcp:8700 jdwp:$$pid ; \
		jdb -attach localhost:8700 -sourcepath src ; \
	done


test2:
	for pid in `adb jdwp` ; do \
		echo $$pid ; \
	done

install:
	adb debug

upload:
	adb install bin/CoMingleMobile-debug.apk
