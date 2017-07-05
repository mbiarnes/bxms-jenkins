// Prepare repository build script
if (true) {
    out.println("Hello World!")
}

// Repository builder script
String shellScript = """make -f Makefile.BRMS repository

sed -i '/^bxms.maven.repo.latest.url=/d' \${HOME}/\${release_prefix}-deliverable-list-staging.properties 
echo "bxms.maven.repo.latest.url=\${rcm_stage_base}/jboss-bpmsuite/\${bpms_product_name}-\${product_version}.\${release_milestone}/jboss-brms-bpmsuite-\${product_version}.GA-maven-repository.zip">>\${HOME}/\${release_prefix}-deliverable-list-staging.properties

cp \${HOME}/\${release_prefix}-deliverable-list-staging.properties \${release_prefix}-deliverable-list-staging.properties
sed -e 's=rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-brms=download.devel.redhat.com/devel/candidates/BRMS=g' -e 's=rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-bpmsuite=download.devel.redhat.com/devel/candidates/BPMS=g' \${release_prefix}-deliverable-list-staging.properties > \${release_prefix}-deliverable-list.properties
"""

// Creates or updates a free style job.
job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-maven-repository-build") {

    // Sets a description for the job.
    description("This job is responsible for offline Maven repository build.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds environment variables to the build.
    environmentVariables {

        // Adds environment variables from a properties file.
        propertiesFile("\${HOME}/${CI_PROPERTIES_FILE}")

        // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
        keepBuildVariables(true)

        // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
        keepSystemVariables(true)
    }

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

        // Adds timestamps to the console log.
        timestamps()
    }

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
        stringParam(parameterName = "task_id", defaultValue = null,
                description = "Brew task id")
    }

    // Allows a job to check out sources from an SCM provider.
    scm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch('${ip_config_branch}')
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    // Adds post-build actions to the job.
    publishers {

        //Archives artifacts with each build.
        archiveArtifacts('workspace/bxms-repository/archive/**/*')

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('workspace/bxms-repository/archive/*.zip,workspace/bxms-repository/archive/*.text,workspace/bxms-repository/archive/*.md5')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('workspace/bxms-repository/archive/')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')

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
                    sourceFiles('workspace/bxms-repository/archive/*.zip,workspace/bxms-repository/archive/*.text,workspace/bxms-repository/archive/*.md5')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('workspace/bxms-repository/archive/')

                    // Sets the destination folder.
                    remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')

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