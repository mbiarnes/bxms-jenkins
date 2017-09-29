import org.jboss.bxms.jenkins.JobTemplate

// Staging script.
def shellScript = """
#Uploading to rcm staging folder
wget \${brms_staging_properties_url} -O \${brms_staging_properties_name} 
wget \${brms_candidate_properties_url} -O \${brms_candidate_properties_name}

ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_version} \
   --deliverable \${release_prefix}-release/brms-deliverable.properties --maven-repo \${bxms_patch_maven_repo_url} \
   --output \${brms_product_name}\
   --release-url=\${rcm_staging_base}/\${brms_staging_path} --output-deliverable-list \${brms_staging_properties_name}
   
ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_version} \
   --deliverable \${release_prefix}-release/bpmsuite-deliverable.properties --maven-repo \${bxms_patch_maven_repo_url} \
   --output \${bpms_product_name}\
   --release-url=\${rcm_staging_base}/\${bpms_staging_path} --output-deliverable-list \${brms_staging_properties_name}

sed -e "s=\${rcm_staging_base}/\${brms_staging_folder}=\${rcm_candidate_base}/\${brms_product_name}=g" \
        -e "s=\${rcm_staging_base}/\${bpms_staging_folder}=\${rcm_candidate_base}/\${bpms_product_name}=g" \
        \${brms_staging_properties_name} > \${brms_candidate_properties_name}
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-stage-bxms-patch") {

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
                    sourceFiles('${brms_product_name}/*.*')
                    removePrefix('${brms_product_name}/')

                    // Sets the destination folder.
                    remoteDirectory('${brms_staging_path}')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${bpms_product_name}/*.*')
                    removePrefix('${bpms_product_name}/')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_staging_path}')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${release_prefix}-deliverable-list*.properties')

                    // Sets the destination folder.
                    remoteDirectory('${brms_staging_path}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${release_prefix}-deliverable-list*.properties')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_staging_path}/')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)