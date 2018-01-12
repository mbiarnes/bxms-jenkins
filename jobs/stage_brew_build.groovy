import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
set -x

case "\${PRODUCT_NAME}" in
    RHDM )
        prod_artifact_version=\${product1_artifact_version}
        prod_shipped_file_deliver_version=\${product1_shipped_file_deliver_version}
        prod_assembly_maven_repo_url=\${product1_assembly_maven_repo_url}
        prod_deliverable_template=\${product1_deliverable_template}
        prod_staging_path=\${product1_staging_path}
        ;;
    RHBAS )
        prod_artifact_version=\${product2_artifact_version}
        prod_shipped_file_deliver_version=\${product2_shipped_file_deliver_version}
        prod_assembly_maven_repo_url=\${product2_assembly_maven_repo_url}
        prod_deliverable_template=\${product2_deliverable_template}
        prod_staging_path=\${product2_staging_path}
        ;;
esac

echo "prod_staging_path=\$prod_staging_path" > /tmp/prod_staging_path

ip-tooling/maven-artifact-handler.py --version=\${prod_artifact_version} --override-version \${prod_shipped_file_deliver_version} --maven-repo \${prod_assembly_maven_repo_url} \
  --deliverable \${prod_deliverable_template} --output \${PRODUCT_NAME}

cp ${IP_CONFIG_FILE} \${PRODUCT_NAME}
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-stage-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

    // Allows to parameterize the job.
    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        booleanParam(parameterName = "CLEAN_STAGING_ARTIFACTS", defaultValue = false, description = "WARNING, click this will force remove your artifacts in staging folder!")
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "RHDM",
                description = "Specify product name to switch between configurations.")
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
        // Inject environment variables for staging paths
        environmentVariables {
            propertiesFile("/tmp/prod_staging_path")
        }
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
                        sourceFiles('')
                        // Sets the destination folder.
                        remoteDirectory('${prod_staging_path}')
                        execCommand('if [ "${CLEAN_STAGING_ARTIFACTS}" = "true" ];then \n' +
                                        'rm -vrf  ~/staging/${prod_staging_path}/* \n' +
                                    'fi')
                    }
                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${PRODUCT_NAME}/*.*')
                        removePrefix('${PRODUCT_NAME}/')

                        // Sets the destination folder.
                        remoteDirectory('${prod_staging_path}')
                    }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
