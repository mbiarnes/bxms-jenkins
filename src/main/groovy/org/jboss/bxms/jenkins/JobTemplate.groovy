package org.jboss.bxms.jenkins


class JobTemplate {


    static void addCommonConfiguration(job, CI_PROPERTIES_FILE, NODE_LABEL="release-pipeline"){

        job.with {

            // Label which specifies which nodes this job can run on.
            label(NODE_LABEL)

            // Adds environment variables to the build.
            environmentVariables {
                // Adds environment variables from a properties file.
                propertiesFile("${CI_PROPERTIES_FILE}")

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

            }
            // Adds pre/post actions to the job.
            wrappers {

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
                    branch("master")
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

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-tooling")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("master")

                    // Adds additional behaviors.
                    extensions {

                        // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                        relativeTargetDirectory('bxms-tooling')
                    }
                }

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("master")

                    // Adds additional behaviors.
                    extensions {

                        // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                        relativeTargetDirectory('bxms-jenkins')
                    }
                }
            }
        }
    }
}
