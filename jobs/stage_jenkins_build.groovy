// Staging script.
def shellScript = '''#disable bash tracking mode, too much noise
#set +x

echo $maven_repo_url

rm -rf ip-tooling
git clone  https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git ip-tooling
cd ip-tooling && git checkout ${ip_tooling_branch}
cd -

#Uploading to rcm staging folder
ip-tooling/maven-to-stage.py --version=${product_artifact_version} --override-version ${product_version}.${release_milestone} \\
   --deliverable ${release_prefix}-release/brms-deliverable.properties --maven-repo ${maven_repo_url} \\
   --output ${brms_product_name}-${product_version}.${release_milestone} \\
   --release-url=${rcm_stage_base}/jboss-brms/${brms_product_name}-${product_version}.${release_milestone} --output-deliverable-list ${HOME}/${release_prefix}-deliverable-list.properties
   

ip-tooling/maven-to-stage.py --version=${product_artifact_version} --override-version ${product_version}.${release_milestone} \\
   --deliverable ${release_prefix}-release/bpmsuite-deliverable.properties --maven-repo ${maven_repo_url} \\
   --output ${bpms_product_name}-${product_version}.${release_milestone} \\
   --release-url=${rcm_stage_base}/jboss-bpmsuite/${bpms_product_name}-${product_version}.${release_milestone} --output-deliverable-list ${HOME}/${release_prefix}-deliverable-list.properties

sed -i '/^build.config=/d' ${HOME}/${release_prefix}-deliverable-list.properties && echo "build.config=${rcm_stage_base}/jboss-bpmsuite/${bpms_product_name}-${product_version}.${release_milestone}/${release_prefix}.cfg">>${HOME}/${release_prefix}-deliverable-list.properties
sed -i '/^DROOLSJBPM_VERSION=/d' ${HOME}/${release_prefix}-deliverable-list.properties && echo "DROOLSJBPM_VERSION=${kie_version}">>${HOME}/${release_prefix}-deliverable-list.properties
sed -i '/^BXMS_VERSION=/d' ${HOME}/${release_prefix}-deliverable-list.properties && echo "BXMS_VERSION=${product_artifact_version}">>${HOME}/${release_prefix}-deliverable-list.properties

cp ${HOME}/${release_prefix}-deliverable-list.properties ${release_prefix}-deliverable-list.properties
'''

// Creates or updates a free style job.
job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-stage-jenkins-build") {

    // Sets a description for the job.
    description("This job is responsible for put brew artifact to staging area.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }

    // Adds environment variables to the build.
    environmentVariables {

        // Adds environment variables from a properties file.
        propertiesFile("\${HOME}/${CI_PROPERTIES_FILE}")

        // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
        keepBuildVariables(true)

        // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
        keepSystemVariables(true)
    }

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

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${brms_product_name}-${product_version}.${release_milestone}/*.*')

                    // Sets the destination folder.
                    remoteDirectory('${brms_stage_folder}')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${bpms_product_name}-${product_version}.${release_milestone}/*.*')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_stage_folder}')
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
