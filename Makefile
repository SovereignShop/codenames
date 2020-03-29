VERSION:=$(shell git rev-parse --short=10 HEAD)

target:
	mkdir -p target

target/classes/codenames/core.class: deps.edn src/**/* target
	rm -fr resources/public/js
	clojure -A:cljs-prod
	clojure -A:build

build: target/classes/codenames/core.class

docker: target/classes/codenames/core.class
	docker build --rm -t jmicahc/codenames:test .

deploy: docker
	docker push jmicahc/codenames:test
	aws ecs update-service --service docker-codenames-service --region us-west-1 --cluster codenames-cluster --force-new-deployment

clean:
	rm -fr resources/public/js
	rm -fr target

.PHONY: build docker clean
