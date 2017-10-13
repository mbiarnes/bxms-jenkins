import org.jboss.bxms.jenkins.JobTemplate

// incremental repository
def incrementalRepositoryString = null

if (PRODUCT_NAME == "bxms64") {

    incrementalRepositoryString = "http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-brms/BRMS-6.4.0.CR2/jboss-brms-bpmsuite-6.4.0.GA-maven-repository/maven-repository"

}
// Repository builder script
def shellScript = """
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Maven repository build started: Build url:\${BUILD_URL}" -f

function appendProp(){
    if [ -z "\$1" ] || [ -z "\$2" ] || [ -z "\$3" ];then
        echo "Param  is not allow empty"
        exit 1
    fi
    sed -i "/^\$1/d" \$3 && echo "\$1=\$2" >> \$3
}

if ! wget \${brms_staging_properties_url} -O \${brms_staging_properties_name} 2>/dev/null ;then
    echo " \${brms_staging_properties_url} isn't available yet"  
fi 
if ! wget \${brms_candidate_properties_url} -O \${brms_candidate_properties_name} 2>/dev/null ;then
  echo " \${brms_candidate_properties_url} isn't available yet"
fi
#append the maven repo url into the properties
override_product_deliver_version=\${product_deliver_version} 
#Uploading to rcm staging folder
if [ \${release_milestone:0:2} = "CR" ];then
    override_product_deliver_version=\${product_version}\${availability}
fi
bxms_maven_repo_name=jboss-bpmsuite-{override_product_deliver_version}-maven-repository.zip
appendProp "bxms.maven.repo.latest.url" \${rcm_staging_base}/\${bpms_staging_path}/\${bxms_maven_repo_name} \$brms_staging_properties_name


if [ \$release_type = "patch" ];then
    bxms_maven_repo_name=jboss-bpmsuite-{override_product_deliver_version}-incremental-maven-repository.zip
    appendProp "bxms.maven.incremental.repo.latest.url" \${rcm_staging_base}/\${bpms_staging_path}/\${bxms_incr_maven_repo_name} \$brms_staging_properties_name
fi

sed -e "s=\${rcm_staging_base}/\${brms_staging_folder}=\${rcm_candidate_base}/\${brms_product_name}=g" \
        -e "s=\${rcm_staging_base}/\${bpms_staging_folder}=\${rcm_candidate_base}/\${bpms_product_name}=g" \
        \${brms_staging_properties_name} > \${brms_candidate_properties_name}
        
make CFG=${IP_CONFIG_FILE} MAVEN_REPOSITORY_BUILDER_SCRIPT=\${repository_builder_script} -f \${makefile} repository
#if [ "\$release_type" = "default" ];then
#TODO rename the maven repository zip to make it consistent with others
#    cd workspace/\${release_prefix}-repository/archive
#    for file in jboss-brms-bpmsuite-*
#    do
#      mv "\$file" "\${file/\${product_version}.\${release_milestone}/\${product_deliver_version}}"
#    done
#fi
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-maven-repository-build") {

    // Sets a description for the job.
    description("This job is responsible for building the offline maven repository zip for MRRC.")

    // Allows to parameterize the job.
    parameters {

        // Defines a simple boolean parameter.
        booleanParam(parameterName = "CLEAN_CARTOGRAPHER_CACHE", defaultValue = false,
                description = "Tick if you want to wipe local Cartographer cache to send over new requests.")

        // Defines a simple boolean parameter.
        booleanParam(parameterName = "DELETE_CARTOGRAPHER_WORKSPACE", defaultValue = false,
                description = "Tick if you want to wipe remote Cartographer workspace containing any resolved dependency graph.")

        // Defines a simple boolean parameter.
        booleanParam(parameterName = "GEN_REPORT", defaultValue = true,
                description = "Tick if you want to generate report for the newly created repository.")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "INCREMENTAL_REPO_FOR", defaultValue = incrementalRepositoryString,
                description = "List of repositories to exclude. They can be online repository urls or online available zip files in format <url to the zip>:<relative path to repo root inside the zip<. Each repository is supposed to be put on a new line.")

    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    wrappers {
        // Deletes files from the workspace before the build starts.
        preBuildCleanup(){
            includePattern('workspace/**')
            deleteDirectories()
        }

    }
    // Adds post-build actions to the job.
    publishers {

        //Archives artifacts with each build.
        archiveArtifacts('workspace/${release_prefix}-repository/archive/**/*')

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                if (PRODUCT_NAME == "intpack-fuse63-bxms64") {
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('workspace/${release_prefix}-repository/archive/*.zip,workspace/bxms-repository/archive/*.text,workspace/bxms-repository/archive/*.md5')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('workspace/${release_prefix}-repository/archive/')

                        // Sets the destination folder.
                        remoteDirectory('${product_stage_folder}/${product_name}-${product_version}')

                        // Specifies a command to execute on the remote server.
                        execCommand('unzip ' +
                                '-o ~/staging/${product_stage_folder}/${product_name}-${product_version}/maven-repository-report.zip ' +
                                '-d ~/staging/${product_stage_folder}/${product_name}-${product_version}/ && rm ' +
                                '-f ~/staging/${product_stage_folder}/${product_name}-${product_version}/maven-repository-report.zip')
                    }
                } else {
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('workspace/${release_prefix}-repository/archive/*.zip,workspace/${release_prefix}-repository/archive/*.text,workspace/${release_prefix}-repository/archive/*.md5')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('workspace/${release_prefix}-repository/archive/')

                        // Sets the destination folder.
                        remoteDirectory('${brms_staging_path}')

                        // Specifies a command to execute on the remote server.
                        execCommand('unzip ' +
                                '-o ~/staging/${brms_staging_path}/maven-repository-report.zip ' +
                                '-d ~/staging/${brms_staging_path}' +
                                '&& rm ' +
                                '-f ~/staging/${brms_staging_path}/maven-repository-report.zip')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('workspace/${release_prefix}-repository/archive/*.zip,workspace/${release_prefix}-repository/archive/*.text,workspace/${release_prefix}-repository/archive/*.md5')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('workspace/${release_prefix}-repository/archive/')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_staging_path}')

                        // Specifies a command to execute on the remote server.
                        execCommand('unzip ' +
                                '-o ~/staging/${bpms_staging_path}/maven-repository-report.zip ' +
                                '-d ~/staging/${bpms_staging_path}/ ' +
                                '&& rm ' +
                                '-f ~/staging/${bpms_staging_path}/maven-repository-report.zip')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-deliverable-list*.properties')

                        // Sets the destination _path.
                        remoteDirectory('${brms_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-deliverable-list*.properties')

                        remoteDirectory('${bpms_staging_path}')
                    }
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)