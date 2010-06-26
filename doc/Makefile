V = 01
PV = 00
DIFF = nigori-protocol-$(PV)-$V-diff.html
DIST = nigori-overview.pdf nigori-protocol-$V.html nigori-protocol-$V.txt $(DIFF)

all: nigori-overview.dvi nigori-protocol-$V.html

publish: $(DIST)
	rsync -v $(DIST) sump2.links.org:files

diff:: $(DIFF)

nigori-protocol-$V.xml: nigori-protocol.xml
	cp -f nigori-protocol.xml nigori-protocol-$V.xml
	chmod -w nigori-protocol-$V.xml

$(DIFF): nigori-protocol-$(PV).txt nigori-protocol-$V.txt
	rfcdiff --stdout nigori-protocol-$(PV).txt nigori-protocol-$V.txt > $(DIFF)

.SUFFIXES: .dvi .tex .pdf .bib .html .xml .txt

.xml.html:
	DISPLAY= xml2rfc $* $*.html

.xml.txt:
	DISPLAY= xml2rfc $* $*.txt

.tex.dvi:
	latex $*.tex
#	bibtex $*
	latex $*.tex
	latex $*.tex

.tex.pdf:
	latex $*.tex
#	bibtex capabilities
#	latex capabilities.tex
	pdflatex $*.tex
