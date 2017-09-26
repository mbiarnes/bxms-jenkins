import org.jboss.bxms.jenkins.JobTemplate

String shellScript = '''
mkdir jboss-bpmsuite-${product_version}${openshift_image_version_suffix}-openshift
cd jboss-bpmsuite-${product_version}${openshift_image_version_suffix}-openshift

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
org.jboss.ip:jboss-bpmsuite::business-central-monitoring-ee7:zip
org.jboss.ip:jboss-bpmsuite::smart-router:jar
org.jboss.ip:jboss-bpmsuite::execution-server-ee7:zip
org.jboss.ip:jboss-bpmsuite::execution-server-controller-ee7:zip""" >>download_list.properties
fi
maven_repo_url="http://download-node-02.eng.bos.redhat.com/brewroot/repos/jb-bxms-7.0-maven-build/latest/maven/"
./maven-to-stage.py --version=${product_artifact_version} --override-version ${product_version}${openshift_image_version_suffix} --deliverable download_list.properties --maven-repo ${maven_repo_url} --output BPMS-${product_version}${openshift_image_version_suffix}

rm -f download_list.properties maven-to-stage.py
if [ $? -ne 0 ]
then
    echo "Failed to remove files"
    exit 1;
fi
if [ ${skipDocker} = "false"  ]
then
#Clean  docker images
for i in $(docker images -q);do docker rmi $i; done

#Download image config/sources
wget https://github.com/jboss-openshift/application-templates/archive/bpmsuite70-wip.zip ;unzip -j bpmsuite70-wip.zip */bpmsuite/* -d application-template;rm -f bpmsuite*.zip;
wget https://github.com/jboss-container-images/jboss-bpmsuite-7-image/archive/bpmsuite70-dev.zip; unzip bpmsuite70-dev.zip;mv jboss-bpmsuite-7-image-bpmsuite70-dev standalone-image-source; rm -f bpmsuite70-*.zip; 
wget https://github.com/jboss-container-images/jboss-bpmsuite-7-openshift-image/archive/bpmsuite70-dev.zip; unzip bpmsuite70-dev.zip;mv jboss-bpmsuite-7-openshift-image-bpmsuite70-dev openshift-image-source;rm -f bpmsuite70-*.zip;

if ! wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/brms-release/bpmsuite-image-stream.json -P application-template/
then
exit 1;
fi
sed -i "s/replace_image_version/${openshift_image_version}/g" application-template/bpmsuite-image-stream.json            
#Define the internal docker registry            
docker_registry=docker-registry.engineering.redhat.com

docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${openshift_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${openshift_image_version} >bpmsuite70-businesscentral-openshift-${openshift_image_version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${openshift_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${openshift_image_version} >bpmsuite70-executionserver-openshift:${openshift_image_version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${openshift_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${openshift_image_version} >bpmsuite70-standalonecontroller-openshift-${openshift_image_version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${openshift_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${openshift_image_version} >bpmsuite70-smartrouter-openshift-${openshift_image_version}.tar
docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-monitoring-openshift:${openshift_image_version}
docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-monitoring-openshift:${openshift_image_version} >bpmsuite70-businesscentral-monitoring-openshift-${openshift_image_version}.tar

cd ..
zip -5 -r  jboss-bpmsuite-${product_version}${product_deliver_version}-openshift.zip jboss-bpmsuite-${product_version}${openshift_image_version_suffix}-openshift/
md5sum jboss-bpmsuite-${product_version}${product_deliver_version}-openshift.zip >jboss-bpmsuite-${product_version}${product_deliver_version}-openshift.zip.md5
fi
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-export-openshift-images") {

    // Sets a description for the job.
    description("This job is responsible for exporting openshift images to zip files.")

    label("docker")

    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        booleanParam(parameterName = "skipDocker", defaultValue = false , description = "Skip package Openshift Image")
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
        archiveArtifacts('**/*.zip,**/*-smart-router.jar')

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('jboss-bpmsuite-${product_version}${openshift_image_version_suffix}-openshift.zip*')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_staging_path}/')
                }

            }
        }
    }

}
JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)


