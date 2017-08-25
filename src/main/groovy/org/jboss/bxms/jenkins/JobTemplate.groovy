package org.jboss.bxms.jenkins


class JobTemplate {

    static void addCommonConfiguration(job, CI_PROPERTIES_FILE, PRODUCT_NAME) {

        job.with {

            // Label which specifies which nodes this job can run on.
            label("pvt-static")

            // Adds environment variables to the build.
            environmentVariables {

                // Adds environment variables from a properties file.
                propertiesFile("\${HOME}/${CI_PROPERTIES_FILE}")

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

                // Prepare additional variables
                if (PRODUCT_NAME == "intpack-fuse63-bxms64") {

                    // Adds an environment variable to the build.
                    env("COMBINATION", "fuse63-bxms64")
                }
            }
            // Adds pre/post actions to the job.
            wrappers {

                // Deletes files from the workspace before the build starts.
                preBuildCleanup()

                // Adds timestamps to the console log.
                timestamps()
            }

            // Manages how long to keep records of the builds.
            logRotator {

                // If specified, only up to this number of build records are kept.
                numToKeep(50)

                // If specified, only up to this number of builds have their artifacts retained.
                artifactNumToKeep(5)
            }
        }
    }

    static void addIpToolingScmConfiguration(job, ipConfigBranch = "master") {

        job.with {

            // Allows a job to check out sources from multiple SCM providers.
            multiscm {

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch(ipConfigBranch)
                }

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
            }
        }
    }
}