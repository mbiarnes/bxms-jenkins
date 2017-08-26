String shellScript = '''
#Clean  docker images
for i in $(docker images -q);do docker rmi $i; done

mkdir jboss-bpmsuite-7.0.0-openshift
cd jboss-bpmsuite-7.0.0-openshift

#Download image config
if ! wget https://raw.githubusercontent.com/jboss-openshift/application-templates/bpmsuite-wip/bpmsuite/bpmsuite70-full-mysql.json -O bpmsuite-full-mysql.json
then
        exit 1
fi
if ! wget https://raw.githubusercontent.com/jboss-openshift/application-templates/bpmsuite-wip/secrets/bpmsuite-app-secret.json
then
exit 1;
fi
if ! wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/brms-release/bpmsuite-image-stream.json
then
exit 1;
fi
sed -e 's/${image_version}/1.0.Beta01/g' bpmsuite-image-stream.json

version=1.0
docker_registry=docker-registry.engineering.redhat.com

docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${version} >bpmsuite70-businesscentral-openshift-${version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${version} >bpmsuite70-executionserver-openshift:${version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${version} >bpmsuite70-standalonecontroller-openshift-${version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${version} >bpmsuite70-smartrouter-openshift-${version}.tar

cd ..
zip -5 -r  jboss-bpmsuite-7.0.0-openshift.zip jboss-bpmsuite-7.0.0-openshift/
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-export-openshift-images") {

    // Sets a description for the job.
    description("This job is responsible for exporting openshift images to zip files.")

    label("docker")

    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "version", defaultValue = null , description = "Input the bxms 7 openshift image version to export")
    }
    
    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }
    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    // Adds post-build actions to the job.
    publishers {

        //Archives artifacts with each build.
        archiveArtifacts('**/*.zip')
    }
}

