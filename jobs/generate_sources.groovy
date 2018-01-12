import org.jboss.bxms.jenkins.JobTemplate

shellScript = """
# Kerberos authentication
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

# Workaround for variable name conflict between Jenkins and ip-tooling
unset WORKSPACE


# Make sources
make CFG=${IP_CONFIG_FILE} SOURCES=1 POMMANIPEXT=bxms-bom SRCDIR=src -f Makefile.BRMS \${PRODUCT_NAME,,}

## Prepare sources for delivery ##
cd workspace

# Remove settings.xml
# TODO It's a fast fix. It should be more generic.
#rm -f src/errai-parent*/settings.xml

case "\${PRODUCT_NAME}" in
    RHDM )
        prod_artifact_version=\${product1_artifact_version}
        prod_staging_path=\${product1_staging_path}
        prod_sources_name=\${product1_sources_name}
        rm -rf src/jbpm-wb-\${kie_version}

        ;;
    RHBAS )
        prod_artifact_version=\${product2_artifact_version}
        prod_sources_name=\${product2_sources_name}
        prod_staging_path=\${product2_staging_path}
        ;;
esac

rm -rf src/bxms-license-\${prod_artifact_version} \
       src/bxms-maven-repo-root-\${prod_artifact_version} \
       src/errai-parent-\${errai_version} \
       src/bxms

rm -rf src/kie-parent-\${kie_version}/RELEASE-README.md

# Create sources archive
zip -r -5 --quiet \${prod_sources_name}-\${prod_shipped_file_deliver_version} src/
echo "prod_staging_path=\${prod_staging_path}" > /tmp/prod_staging_path
echo "prod_sources_name=\${prod_sources_name}" >> /tmp/prod_staging_path
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-generate-sources") {

    // Sets a description for the job.
    description("This job is responsible for generating product sources.")
    parameters {
        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "RHDM",
                description = "Specify product name to switch between configurations.")
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
        // Inject environment variables for staging paths
        environmentVariables {
            propertiesFile("/tmp/prod_staging_path")
        }
    }

    // Adds post-build actions to the job.
    publishers {
        //Archives artifacts with each build.
        archiveArtifacts('workspace/\${prod_sources_name}.zip')

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('workspace/\${prod_sources_name}')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('workspace/')

                    // Sets the destination path.
                    remoteDirectory('${prod_staging_path}')
                }
            }
        }
    }
}

//Make sure that label is exclusive to avoid multiple job run into the same workspace
JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, "release-pipeline && exclusive")
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
