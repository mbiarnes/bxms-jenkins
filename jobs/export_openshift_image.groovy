import org.jboss.bxms.jenkins.JobTemplate

String shellScript = '''
mkdir jboss-bpmsuite-${product_version}-openshift
cd jboss-bpmsuite-${product_version}-openshift

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
org.kie.rhap:rhbas::business-central-standalone:jar:rhbas.business-central.standalone.latest.url
org.kie.rhap:rhbas::business-central-eap7:zip:rhbas.business-central-eap7.latest.url
org.kie.rhap:rhbas::add-ons:zip:rhbas.addons.latest.url
org.kie.rhap:rhbas::execution-server-ee7:zip:rhbas.execution-server.ee7.latest.url""" >>download_list.properties
fi
maven_repo_url="http://download-node-02.eng.bos.redhat.com/brewroot/repos/\${brew_target}/latest/maven/"
./maven-to-stage.py --version=${product_artifact_version} --override-version ${product_version}${availability} --deliverable download_list.properties --maven-repo ${maven_repo_url} --output BPMS-${product_version}${availability}

rm -f download_list.properties maven-to-stage.py
if [ $? -ne 0 ]
then
    echo "Failed to remove files"
    exit 1;
fi
if [ ${skipPackage} = "false"  ]
then

#Download image config/sources
wget https://github.com/jboss-openshift/application-templates/archive/bpmsuite-wip.zip;unzip -j bpmsuite-wip.zip */bpmsuite/bpmsuite70-businesscentral-monitoring-with-smartrouter.json */bpmsuite/bpmsuite70-executionserver-postgresql.json */bpmsuite/bpmsuite70-executionserver-externaldb.json -d application-template;rm -f bpmsuite*.zip;
wget https://github.com/jboss-container-images/jboss-bpmsuite-7-image/archive/${openshift_image_tag}.zip; unzip ${openshift_image_tag}.zip;mv jboss-bpmsuite-7-image-${openshift_image_tag} standalone-image-source; rm -f ${openshift_image_tag}.zip;rm -f standalone-image-source/.gitignore;
wget https://github.com/jboss-container-images/jboss-bpmsuite-7-openshift-image/archive/${openshift_image_tag}.zip; unzip ${openshift_image_tag}.zip;mv jboss-bpmsuite-7-openshift-image-${openshift_image_tag} openshift-image-source;rm -f ${openshift_image_tag}.zip;rm -f openshift-image-source/.gitignore;

if ! wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/brms-release/bpmsuite-image-stream.json -P application-template/
then
exit 1;
fi
sed -i "s/replace_image_version/${openshift_image_version}/g" application-template/bpmsuite-image-stream.json            

#Clean  docker images
#for i in $(docker images -q);do docker rmi $i; done

#Define the internal docker registry            
#docker_registry=docker-registry.engineering.redhat.com

#docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${openshift_image_version}
#docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${openshift_image_version} >bpmsuite70-businesscentral-openshift-${openshift_image_version}.tar
#docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${openshift_image_version}
#docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${openshift_image_version} >bpmsuite70-executionserver-openshift:${openshift_image_version}.tar
#docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${openshift_image_version}
#docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${openshift_image_version} >bpmsuite70-standalonecontroller-openshift-${openshift_image_version}.tar
#docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${openshift_image_version}
#docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${openshift_image_version} >bpmsuite70-smartrouter-openshift-${openshift_image_version}.tar
#docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-monitoring-openshift:${openshift_image_version}
#docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-monitoring-openshift:${openshift_image_version} >bpmsuite70-businesscentral-monitoring-openshift-${openshift_image_version}.tar

cd ..
zip -5 -r  jboss-bpmsuite-${product_version}${availability}-openshift.zip jboss-bpmsuite-${product_version}-openshift/
md5sum jboss-bpmsuite-${product_version}${availability}-openshift.zip >jboss-bpmsuite-${product_version}${availability}-openshift.zip.md5
fi
'''

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-export-openshift-images") {

    // Sets a description for the job.
    description("This job is responsible for exporting openshift images to zip files.")

    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        booleanParam(parameterName = "skipPackage", defaultValue = false , description = "Skip package Openshift Image")
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
                    sourceFiles('jboss-bpmsuite-${product_version}${availability}-openshift.zip*')

                    // Sets the destination folder.
                    remoteDirectory('${product2_staging_path}/')
                }

            }
        }
    }

}
JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)


