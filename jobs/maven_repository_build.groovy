import org.jboss.bxms.jenkins.JobTemplate

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
appendProp "bxms.maven.repo.latest.url" \${rcm_staging_base}/\${bpms_staging_folder}/\${bxms_maven_repo_name} \$brms_staging_properties_name
appendProp "bxms.maven.repo.latest.url" \${rcm_candidate_base}/\${bpms_product_name}/\${bxms_maven_repo_name} \$brms_candidate_properties_name


if [ \$release_type = "patch" ];then
    appendProp "bxms.maven.incremental.repo.latest.url" \${rcm_staging_base}/\${bpms_staging_folder}/\${bxms_incr_maven_repo_name} \$brms_staging_properties_name
    appendProp "bxms.maven.incremental.repo.latest.url" \${rcm_candidate_base}/\${bpms_product_name}/\${bxms_incr_maven_repo_name} \$brms_candidate_properties_name
fi
#TODO rename the maven repository zip to make it consistent with others
make CFG=${IP_CONFIG_FILE} MAVEN_REPOSITORY_BUILDER_SCRIPT=\${repository_builder_script} -f \${makefile} repository
#TODO rename
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
                        sourceFiles('${brms_staging_properties_name},${bpms_candidate_properties_name}')

                        // Sets the destination _path.
                        remoteDirectory('${brms_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${brms_staging_properties_name},${bpms_candidate_properties_name}')

                        remoteDirectory('${bpms_staging_path}')
                    }
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)