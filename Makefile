.PHONY: all

all: info gen-version

branch := $(shell git rev-parse --abbrev-ref HEAD)
commit := $(shell git rev-parse --short HEAD)

info:
	@echo 'SHELL='$(SHELL)
	@echo 'branch='$(branch)
	@echo 'commit='$(commit)

gen-version:
	git describe --tags --always
	git describe --tags --always | sed 's/v/ /g' | sed 's/\./ /g' | sed 's/-/ /g' | awk '{print ($$1*16777216)+($$2*65536)+($$3*256)+$$4}'

#make gen v=v2.0.0
gen:
	echo $(v) | sed 's/v/ /g' | sed 's/\./ /g' | sed 's/-/ /g' | awk '{print "{\"version_code\": " ($$1*16777216)+($$2*65536)+($$3*256)+$$4 ", \"version_name\": \"" "v$(v)" "\"}"}' > version.json