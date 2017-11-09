
def shell_script = """
cfg=${IP_CONFIG_FILE}
echo "Validating upstreams in ${IP_CONFIG_FILE}"
VALIDATE_ONLY=true LOCAL=1 CFG=./${IP_CONFIG_FILE} MVN_LOCAL_REPO=/jboss-prod/m2/bxms-dev-repo POMMANIPEXT=bxms-bom make -f Makefile.BRMS brms-installer bpms-installer
"""

job('bxms-validate-upstream-sources') {
    description("Validate if upstream source configuration is proper")

    multiscm {
        // Adds a Git SCM source.
        git {
            // Adds a remote.
            remote {
                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
            }
            // Specify the branches to examine for changes and to build.
            branch("master")
        }
        git {
            // Adds a remote.
            remote {
                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling")
            }
            // Specify the branches to examine for changes and to build.
            branch("master")
            // Adds additional behaviors.
            extensions {
                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                relativeTargetDirectory('ip-tooling')
            }
        }
    }
    label('bxms-nightly')

    // build steps
    steps {
        shell(shell_script)
    }
    // clear workspace
    wrappers {
        preBuildCleanup()
    }
    // Adds post-build actions to the job.
    publishers {
        //Archives artifacts with each build.
        archiveArtifacts('brms-bpmsuite/*/target/*.zip')
    }
}

