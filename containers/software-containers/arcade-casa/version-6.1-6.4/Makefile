VERSIONS_OLD = \
 	casa-6.3.0-48 \
 	casa-6.2.0-124 \
 	casa-6.1.0-118

VERSIONS_PIPELINE = \
 	6.2.1-7-pipeline-2021.2.0.128

VERSIONS = \
	casa-6.4.0-16-py3.8

DOCKER_REPO_BASE=images.canfar.net/arcade/casa

.PHONY: build clean run 

all: build-old build-pipeline build

build-old: 
	@- $(foreach V,$(VERSIONS_OLD), \
		./download.sh $(V) old ; \
		docker build --build-arg CASA_RELEASE=$(V) -t ${DOCKER_REPO_BASE}:$(V) .; \
	)

build-pipeline: 
	@- $(foreach V,$(VERSIONS_PIPELINE), \
                ./download.sh $(V) pipeline ; \
                docker build --build-arg CASA_RELEASE=$(V) -t ${DOCKER_REPO_BASE}:$(V) .; \
	)

build: 
	@- $(foreach V,$(VERSIONS), \
                ./download.sh $(V) current ; \
                docker build --build-arg CASA_RELEASE=$(V) -t ${DOCKER_REPO_BASE}:$(V) .; \
	)

clean-old:
	@- $(foreach V,$(VERSIONS_OLD), \
		docker rmi ${DOCKER_REPO_BASE}:$(V) ; \
	)

clean-pipeline:
	@- $(foreach V,$(VERSIONS_PIPELINE), \
                docker rmi ${DOCKER_REPO_BASE}:$(V) ; \
	)

clean:
	@- $(foreach V,$(VERSIONS), \
                docker rmi ${DOCKER_REPO_BASE}:$(V) ; \
	)

clean-all: clean-old clean-pipeline clean 

upload-old: build-old
	@- $(foreach V,$(VERSIONS_OLD), \
	  docker push ${DOCKER_REPO_BASE}:$(V) ; \
	)

upload-pipeline: build-pipeline
	@- $(foreach V,$(VERSIONS_PIPELINE), \
                docker push ${DOCKER_REPO_BASE}:$(V) ; \
	)

upload: build
	@- $(foreach V,$(VERSIONS), \
                docker push ${DOCKER_REPO_BASE}:$(V) ; \
	)

upload-all: upload-old upload-pipeline upload 
