import org.jboss.bxms.jenkins.JobTemplate

// Computing differences between streams

// incremental repository
def incrementalRepositoryString = null

if (PRODUCT_NAME == "bxms64") {

    incrementalRepositoryString = "http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-brms/BRMS-6.4.0.CR2/jboss-brms-bpmsuite-6.4.0.GA-maven-repository/maven-repository"

}

// Repository builder script
def shellScript = """make CFG=${IP_CONFIG_FILE} MAVEN_REPOSITORY_BUILDER_SCRIPT=${REPOSITORY_BUILDER_SCRIPT} -f ${IP_MAKEFILE} repository

sed -i '/^bxms.maven.repo.latest.url=/d' \${HOME}/\${release_prefix}-deliverable-list-staging.properties
sed -i '/^bxms.maven.incremental-repo.latest.url=/d' \\${HOME}/\\${release_prefix}-deliverable-list-staging.properties
echo "bxms.maven.repo.latest.url=\${rcm_stage_base}/\${bpms_stage_folder}/\${bpms_product_name}-\${product_version}.\${release_milestone}/jboss-brms-bpmsuite-\${product_version}.GA-maven-repository.zip">>\${HOME}/\${release_prefix}-deliverable-list-staging.properties
echo "bxms.maven.repo.latest.url=\\${rcm_stage_base}/\\${bpms_stage_folder}/\\${bpms_product_name}-\\${product_version}.\\${release_milestone}/jboss-brms-bpmsuite-\\${product_version}.GA-incremental-maven-repository.zip">>\\${HOME}/\\${release_prefix}-deliverable-list-staging.properties

cp \${HOME}/\${release_prefix}-deliverable-list-staging.properties \${release_prefix}-deliverable-list-staging.properties
sed -e 's=rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/\${brms_stage_folder}=download.devel.redhat.com/devel/candidates/BRMS=g' -e 's=rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-bpmsuite=download.devel.redhat.com/devel/candidates/BPMS=g' \${release_prefix}-deliverable-list-staging.properties > \${release_prefix}-deliverable-list.properties
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-maven-repository-build") {

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
                        remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')

                        // Specifies a command to execute on the remote server.
                        execCommand('unzip ' +
                                '-o ~/staging/${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/maven-repository-report.zip ' +
                                '-d ~/staging/${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/ ' +
                                '&& rm ' +
                                '-f ~/staging/${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/maven-repository-report.zip')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('workspace/${release_prefix}-repository/archive/*.zip,workspace/${release_prefix}-repository/archive/*.text,workspace/${release_prefix}-repository/archive/*.md5')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('workspace/${release_prefix}-repository/archive/')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')

                        // Specifies a command to execute on the remote server.
                        execCommand('unzip ' +
                                '-o ~/staging/${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/maven-repository-report.zip ' +
                                '-d ~/staging/${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/ ' +
                                '&& rm ' +
                                '-f ~/staging/${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/maven-repository-report.zip')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')
                    }
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)