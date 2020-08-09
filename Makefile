.PHONY: default
default: all

.PHONY: all
all: build test lint

# ------------------------------------------------------------------------------
# BUILD
# ------------------------------------------------------------------------------

DIR := ${CURDIR}

build_dir := $(DIR)/bin

bin/:
	@mkdir -p $@

.PHONY: build
build:
	@go build -o $(build_dir)/site ./cmd/site

.PHONY: build-docker
build-docker:
	@docker build -t site -f ./build/Dockerfile .

# ------------------------------------------------------------------------------
# RUN
# ------------------------------------------------------------------------------

.PHONY: run
run: build
	@$(build_dir)/site run --config config.yaml

.PHONY: run-docker
run-docker: build-docker
	@docker run --rm -it --network=host site run --config config.yaml

# ------------------------------------------------------------------------------
# TEST
# ------------------------------------------------------------------------------

.PHONY: test
test: test-go test-go-race

.PHONY: test-go
test-go:
	@go test ./...

.PHONY: test-go-race
test-go-race:
	@go test -race ./...

# ------------------------------------------------------------------------------
# LINT
# ------------------------------------------------------------------------------

git_dirty := $(shell git status -s)

git-clean-check:
ifneq ($(git_dirty),)
	git diff
	@echo "Git repository is dirty!"
	@false
endif

.PHONY: lint
lint: go-fmt go-vet go-mod-tidy-check

.PHONY: go-fmt
go-fmt:
	@go fmt ./...

.PHONY: go-vet
go-vet:
	@go vet ./...

.PHONY: go-mod-tidy
go-mod-tidy:
	@go mod tidy

.PHONY: go-mod-tidy-check
go-mod-tidy-check: go-mod-tidy
	@$(MAKE) -s git-clean-check
