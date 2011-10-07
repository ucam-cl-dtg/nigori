A = /home/ben/software/unpacked/google_appengine
AS = python2.5 $(A)/dev_appserver.py

test:
	$(AS) -d .

clear:
	$(AS) --clear_datastore .

upload:
	python2.5 $(A)/appcfg.py update .

2:
	mkdir -p ../server2
	cp nigori-server.py nigori.py gcd.py app.yaml ../server2
#	$(AS) -d ../server2

