import org.jboss.bxms.jenkins.JobTemplate

// Staging script.
def shellScript = """
#Uploading to rcm staging folder

ip-tooling/maven-artifact-handler.py --version=\${product1_artifact_version} --override-version \${product1_version} \
   --deliverable \${product1_deliverable_template} --maven-repo \${bxms_patch_maven_repo_url} \
   --output \${product1_name}


ip-tooling/maven-artifact-handler.py --version=\${product2_artifact_version} --override-version \${product2_version} \
   --deliverable \${product2_deliverable_template} --maven-repo \${bxms_patch_maven_repo_url} \
   --output \${product2_name}
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-stage-bxms-patch") {

    // Sets a description for the job.
    description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    wrappers {
        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

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
                    sourceFiles('${product1_name}/*.*')
                    removePrefix('${product1_name}/')

                    // Sets the destination folder.
                    remoteDirectory('${product1_staging_path}')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${product2_name}/*.*')
                    removePrefix('${product2_name}/')

                    // Sets the destination folder.
                    remoteDirectory('${product2_staging_path}')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${release_code}-deliverable-list*.properties')

                    // Sets the destination folder.
                    remoteDirectory('${product1_staging_path}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${release_code}-deliverable-list*.properties')

                    // Sets the destination folder.
                    remoteDirectory('${product2_staging_path}/')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
