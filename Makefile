.PHONY: all

all: info gen-version

branch := $(shell git rev-parse --abbrev-ref HEAD)
commit := $(shell git rev-parse --short HEAD)

info:
	@echo 'SHELL='$(SHELL)
	@echo 'branch='$(branch)
	@echo 'commit='$(commit)

gen-version:
	@echo 'major='$$(cat version.json)

#make gen major=2
gen:
	echo "{\"major\": $(major)}" > version.json