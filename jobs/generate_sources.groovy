import org.jboss.bxms.jenkins.JobTemplate

shellScript = """
# Kerberos authentication
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

# Workaround for variable name conflict between Jenkins and ip-tooling 
unset WORKSPACE

# Make sources
make CFG=${IP_CONFIG_FILE} SOURCES=1 POMMANIPEXT=bxms-bom SRCDIR=src -f Makefile.BRMS \${product1_lowcase} \${product2_lowcase}
make CFG=common.cfg SOURCES=1 SRCDIR=src -f Makefile.COMMON mvel-2.4.0


## Prepare sources for delivery ##
cd workspace

# Remove settings.xml
# TODO It's a fast fix. It should be more generic.
#rm -f src/errai-parent*/settings.xml

# Create sources archive
zip -r sources.zip src/
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-generate-sources") {

    // Sets a description for the job.
    description("This job is responsible for generating product sources.")

    // Adds pre/post actions to the job.
    wrappers {

                // Deletes files from the workspace before the build starts.
                preBuildCleanup()
            }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    // Adds post-build actions to the job.
    publishers {
        //Archives artifacts with each build.
        archiveArtifacts('workspace/sources.zip')

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('workspace/sources.zip')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('workspace/')

                    // Sets the destination path.
                    remoteDirectory('${product1_staging_path}')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('workspace/sources.zip')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('workspace/')

                    // Sets the destination path.
                    remoteDirectory('${product2_staging_path}')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)