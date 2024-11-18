

.PHONY: clean
clean:
	rm -rf build common/build fabric/build neoforge/build


.PHONY: jar
jar:
	./gradlew remapJar
	ls -1 fabric/build/libs
	ls -1 neoforge/build/libs

test:
	./gradlew test

.PHONY: release
release:
	./etc/release.sh

.PHONY: docgen
docgen:
	./etc/docgen.sh

.PHONY: ide
ide:
	./gradlew cleanIdea idea


.PHONY: pr
pr:
	firefox https://github.com/pcal43/gitback/pulls

.PHONY: deps
deps:
	./gradlew -q dependencies --configuration runtimeClasspath


.PHONY: inst
inst:
	rm -f ~/minecraft/instances/1.20.1-forge-dev/.minecraft/mods/fastback*
	rm -f ~/minecraft/instances/1.20.1-fabric-dev/.minecraft/mods/fasback*
	cp fabric/build/libs/fastback*-fabric.jar ~/minecraft/instances/1.20.1-fabric-dev/.minecraft/mods/
	cp forge/build/libs/fastback*-neoforge.jar ~/minecraft/instances/1.20.1-neoforge-dev/.minecraft/mods/

.PHONY: tvf
tvf:
	jar -tvf forge/build/libs/fastback*-neoforge.jar

.PHONY: tvfs
tvfs:
	jar -tvf forge/build/libs/fastback*-shadow.jar
