package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class JenkinsAllJobBuilder {
    String release_code
    String job_type
    String cfg_file

    Job build(DslFactory dslFactory) {

        String _cfg = cfg_file

        //Use .m2/repository as local repo
        String shellScript = """
unset WORKSPACE
DEP_REPO=`pwd`/workspace/.m2
MVN_DEP_REPO=nexus-release::default::file://\${DEP_REPO} LOCAL=1 CFG=./${_cfg} POMMANIPEXT=brms-bom make -f Makefile.BRMS bxms-maven-repo-root
"""

        dslFactory.folder(release_code + "-jenkins-" + job_type + "-pipeline")
        dslFactory.job(release_code + "-jenkins-" + job_type + "-pipeline/" + release_code + "-all") {
            it.description "This job is a seed job for generating " + release_code + " " +  job_type + " jenkins full build."
            logRotator {
                numToKeep 8
            }
            label("bxms-nightly")
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
            }

            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }

        }
    }
}
