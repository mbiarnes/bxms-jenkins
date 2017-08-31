String shellScript = '''
#Clean  docker images
for i in $(docker images -q);do docker rmi $i; done

mkdir jboss-bpmsuite-7.0.0-openshift
cd jboss-bpmsuite-7.0.0-openshift

#Download image config/sources
wget https://github.com/jboss-openshift/application-templates/archive/bpmsuite-wip.zip ;unzip -j bpmsuite-wip.zip */bpmsuite/* -d application-template;rm -f bpmsuite-wip.zip;
wget https://github.com/jboss-container-images/jboss-bpmsuite-7-image/archive/bpmsuite70-dev.zip; unzip bpmsuite70-dev.zip;mv jboss-bpmsuite-7-image-bpmsuite70-dev standalone-image-source; rm -f bpmsuite70-dev.zip; 
wget https://github.com/jboss-container-images/jboss-bpmsuite-7-openshift-image/archive/bpmsuite70-dev.zip; unzip bpmsuite70-dev.zip;mv jboss-bpmsuite-7-openshift-image-bpmsuite70-dev openshift-image-source;rm -f bpmsuite70-dev.zip;


if ! wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/brms-release/bpmsuite-image-stream.json -P application-template/
then
exit 1;
fi
sed -e 's/replace_image_version/${bxms_image_version}/g' application-template/bpmsuite-image-stream.json

#download the zip package
if [ ! -e maven-to-stage.py ];
then
    if ! wget http://git.app.eng.bos.redhat.com/git/integration-platform-tooling.git/plain/maven-to-stage.py
    then
        exit 1;
    fi
chmod +x maven-to-stage.py
fi

if [ ! -e download_list.properties ]
then
        echo  """#Format G:A::classifier:package_type 
org.jboss.ip:jboss-bpmsuite::business-central-eap7:zip
org.jboss.ip:jboss-bpmsuite::business-central-monitoring:zip
org.jboss.ip:jboss-bpmsuite::smart-router:jar
org.jboss.ip:jboss-brms-bpmsuite::execution-server-ee7:zip
org.jboss.ip:jboss-brms-bpmsuite::execution-server-controller-ee7:zip""" >>download_list.properties
fi
product_artifact_version=7.0.0.ER-redhat-1
product_version=7.0.0.Beta01
maven_repo_url="http://download-node-02.eng.bos.redhat.com/brewroot/repos/jb-bxms-7.0-maven-build/latest/maven/"
./maven-to-stage.py --version=${product_artifact_version} --override-version ${product_version} --deliverable download_list.properties --maven-repo ${maven_repo_url} --output BPMS-${product_version}
             
#Define the internal docker registry            
docker_registry=docker-registry.engineering.redhat.com

docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:\${bxms_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:\${bxms_image_version} >bpmsuite70-businesscentral-openshift-${version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:\${bxms_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:\${bxms_image_version} >bpmsuite70-executionserver-openshift:${version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:\${bxms_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:\${bxms_image_version} >bpmsuite70-standalonecontroller-openshift-${version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:\${bxms_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:\${bxms_image_version} >bpmsuite70-smartrouter-openshift-${version}.tar

rm -f download_list.properties maven-to-stage.py
if [ $? -ne 0 ]
then
    echo "Failed to remove files"
    exit 1;
fi

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
        stringParam(parameterName = "bxms_image_version", defaultValue = "1.0.Beta01" , description = "Input the bxms 7 openshift image version to export")
        stringParam(parameterName = "product_artifact_version", defaultValue = "7.0.0.LA-redhat-1" , description = "Product maven artifact version looks like 7.0.0.LA-redhat-1")
        stringParam(parameterName = "product_version", defaultValue = "7.0.0.Beta01" , description = "Product Version looks like 7.0.0.Beta01")
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


    }
    // Adds post-build actions to the job.
    publishers {
        //Archives artifacts with each build.
        archiveArtifacts('jboss-bpmsuite-7.0.0-openshift.zip')

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('jboss-bpmsuite-7.0.0-openshift.zip')

                    // Sets the destination folder.
                    remoteDirectory('jboss-bpmsutie/BPMS-7.0.0.LA.ER2/')
                }

            }
        }
    }

}

