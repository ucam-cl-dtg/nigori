all: nigori-overview.dvi

pdf: nigori-overview.pdf

.SUFFIXES: .dvi .tex .pdf .bib

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
