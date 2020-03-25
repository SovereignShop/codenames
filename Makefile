VERSION:=$(shell git rev-parse --short=10 HEAD)

target:
	mkdir -p target

target/classes/codenames/core.class: deps.edn src/**/* target
	rm -fr resources/public/js
	clojure -A:cljs-prod
	clojure -A:build

build: target/classes/codenames/core.class

docker: target/classes/codenames/core.class
	docker build --rm -t jmicahc/rad:${VERSION} .

clean:
	rm -fr resources/public/js
	rm -fr target

.PHONY: build docker clean
