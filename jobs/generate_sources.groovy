import org.jboss.bxms.jenkins.JobTemplate

shellScript = """
# Kerberos authentication
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

# Workaround for variable name conflict between Jenkins and ip-tooling 
unset WORKSPACE

# Make sources
make CFG=brms.cfg SOURCES=1 SRCDIR=src -f Makefile.BRMS kie-wb-distributions kie-docs droolsjbpm-integration ip-brms
make CFG=common.cfg SOURCES=1 SRCDIR=src -f Makefile.COMMON mvel-2.3.2 xmlpull-1.1.4

# Prepare sources for delivery
cd workspace
rm -rf src/bpms-brms
zip -r sources.zip src/
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-generate-sources") {

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

                    // Sets the destination _path.
                    remoteDirectory('${brms_staging_path}')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('workspace/sources.zip')

                    remoteDirectory('${bpms_staging_path}')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
