.PHONY: all

all: info

branch := $(shell git rev-parse --abbrev-ref HEAD)
commit := $(shell git rev-parse --short HEAD)

info:
	@echo 'SHELL='$(SHELL)
	@echo 'branch='$(branch)
	@echo 'commit='$(commit)
