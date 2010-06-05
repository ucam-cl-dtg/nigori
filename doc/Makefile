DIST = nigori-overview.pdf nigori-protocol.html nigori-protocol.txt

all: nigori-overview.dvi nigori-protocol.html

publish: $(DIST)
	rsync -v $(DIST) sump2.links.org:files

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
