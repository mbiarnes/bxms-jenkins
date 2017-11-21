import org.jboss.bxms.jenkins.JobTemplate

// PVT script.
String shellScript = """cd pvt
/jboss-prod/tools/maven-3.3.9-prod/bin/mvn -Dmaven.repo.local=\${dev_maven_repo} \
    surefire-report:report -B -Dproduct.config=\${product2_smoketest_cfg} -Dproduct.version=\${product2_milestone_version} \
    -Dproduct.target=\${product2_shipped_file_deliver_version} -Dreport.filepath=\${product2_pvt_report_basename} clean package

"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-pvt-test-rhbas") {

    // Sets a description for the job.
    description("This job is responsible for executing product validation tests.")

    multiscm {

        // Adds a Git SCM source.
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
            remote {

                // Sets the remote URL.
                url("https://github.com/project-ncl/pvt")
            }
            // Adds additional behaviors.
            extensions {
                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                relativeTargetDirectory("pvt")
            }
            // Specify the branches to examine for changes and to build.
            branch('*/master')
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    // Adds post-build actions to the job.
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)