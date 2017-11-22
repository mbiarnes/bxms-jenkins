import org.jboss.bxms.jenkins.JobTemplate

// incremental repository
def incrementalRepositoryString = null

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

if ! wget \${product2_staging_properties_url} -O \${product2_staging_properties_name} 2>/dev/null ;then
    echo " \${product2_staging_properties_url} isn't available yet"
    touch  \${product2_staging_properties_name}
fi 
if ! wget \${product2_candidate_properties_url} -O \${product2_candidate_properties_name} 2>/dev/null ;then
  echo " \${product2_candidate_properties_url} isn't available yet"
  touch  \${product2_staging_properties_name}
fi
#append the maven repo url into the properties
appendProp "rhbas.maven.repo.latest.url" \${rcm_staging_base}/\${product2_staging_path}/\${product2_maven_repo_name} \$product2_staging_properties_name
appendProp "rhbas.maven.repo.latest.url" \${rcm_candidate_base}/\${product2_candidate_path}/\${product2_maven_repo_name} \$product2_candidate_properties_name


if [ \$release_type = "patch" ];then
    rhbas_incr_maven_repo_name=rhbas-{shipped_file_deliver_version}-incremental-maven-repository.zip
    appendProp "rhbas.maven.incremental.repo.latest.url" \${rcm_staging_base}/\${product2_staging_path}/\${rhbas_incr_maven_repo_name} \$product2_staging_properties_name
    appendProp "rhbas.maven.incremental.repo.latest.url" \${rcm_candidate_base}/\${product2_candidate_path}/\${rhbas_incr_maven_repo_name} \$product2_candidate_properties_name
fi

PROJECT_NAME=\${product2_name} make CFG=${IP_CONFIG_FILE} BUILDER_SCRIPT=\${repository_builder_script} -f \${makefile} repository
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-maven-repository-build-rhbas") {

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
        archiveArtifacts('workspace/${product2_lowcase}-repository/archive/**/*')

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('workspace/${product2_lowcase}-repository/archive/*.zip,workspace/${product2_lowcase}-repository/archive/*.text,workspace/${product2_lowcase}-repository/archive/*.md5')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('workspace/${product2_lowcase}-repository/archive/')

                        // Sets the destination folder.
                        remoteDirectory('${product2_staging_path}')

                        // Specifies a command to execute on the remote server.
                        execCommand('unzip ' +
                                '-o ~/staging/${product2_staging_path}/maven-repository-report.zip ' +
                                '-d ~/staging/${product2_staging_path}/ ' +
                                '&& rm ' +
                                '-f ~/staging/${product2_staging_path}/maven-repository-report.zip')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${product2_lowcase}-deliverable-list*.properties')

                        remoteDirectory('${product2_staging_path}')
                    }
                }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)