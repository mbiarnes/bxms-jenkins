package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class JenkinsAllJobBuilder {
    String release_code
    String job_type
    String job_name

    Job build(DslFactory dslFactory) {

        String _cfg = release_code + ".cfg"
        if (job_type.equals("nightly"))
            _cfg = release_code + "-dev.cfg"

        //Use .m2/repository as local repo
        String shellScript = """
LOCAL=1 CFG=./${_cfg} POMMANIPEXT=brms-bom make -f Makefile.BRMS bxms-maven-repo-root 
"""

        dslFactory.folder(job_name + "-jenkins-" + job_type + "-pipeline")
        dslFactory.job(job_name + "-jenkins-" + job_type + "-pipeline/" + job_name + "-all") {
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

            triggers {
                upstream('a-master-seed', 'SUCCESS')
            }
        }
    }
}
