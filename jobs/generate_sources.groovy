import org.jboss.bxms.jenkins.JobTemplate

shellScript = """
# Kerberos authentication
#kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

case "\${PRODUCT_NAME}" in
    RHDM )
        prod_staging_properties_url=\${product1_staging_properties_url}
        prod_staging_properties_name=\${product1_staging_properties_name}
        prod_candidate_properties_url=\${product1_candidate_properties_url}
        prod_candidate_properties_name=\${product1_candidate_properties_name}
        prod_staging_path=\${product1_staging_path}
        prod_candidate_path=\${product1_candidate_path}
        prod_sources_name=\${product1_sources_name}
        ;;
    RHBAS )
        prod_staging_properties_url=\${product2_staging_properties_url}
        prod_staging_properties_name=\${product2_staging_properties_name}
        prod_candidate_properties_url=\${product2_candidate_properties_url}
        prod_candidate_properties_name=\${product2_candidate_properties_name}
        prod_staging_path=\${product2_staging_path}
        prod_candidate_path=\${product2_candidate_path}
        prod_sources_name=\${product2_sources_name}
        ;;
esac

# Workaround for variable name conflict between Jenkins and ip-tooling
unset WORKSPACE

# Make sources
make CFG=${IP_CONFIG_FILE} SOURCES=1 POMMANIPEXT=bxms-bom SRCDIR=src -f Makefile.BRMS \${PRODUCT_NAME,,}
make CFG=common.cfg SOURCES=1 SRCDIR=src -f Makefile.COMMON mvel-2.4.0

## Prepare sources for delivery ##
cd workspace

# Remove settings.xml
# TODO It's a fast fix. It should be more generic.
#rm -f src/errai-parent*/settings.xml

# Create sources archive
zip -r prod_sources_name.zip src/

if ! wget \${prod_staging_properties_url} -O \${prod_staging_properties_name} 2>/dev/null ;then
    echo " \${prod_staging_properties_url} isn't available yet"
    touch  \${prod_staging_properties_name}
fi
if ! wget \${prod_candidate_properties_url} -O \${prod_candidate_properties_name} 2>/dev/null ;then
  echo " \${prod_candidate_properties_url} isn't available yet"
  touch  \${prod_staging_properties_name}
fi

appendProp "\${PRODUCT_NAME,,}.sources.latest.url" \${rcm_staging_base}/\${prod_staging_path}/\${prod_sources_name} \$prod_staging_properties_name
appendProp "\${PRODUCT_NAME,,}.sources.latest.url" \${rcm_candidate_base}/\${prod_candidate_path}/\${prod_sources_name} \$prod_candidate_properties_name

"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-generate-sources") {

    // Sets a description for the job.
    description("This job is responsible for generating product sources.")
    parameters {
      stringParam(parameterName = "PRODUCT_NAME", defaultValue = "",
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
                    sourceFiles('workspace/${prod_sources_name}')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('workspace/')

                    // Sets the destination path.
                    remoteDirectory('${prod_staging_path}')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
