package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS General build job , eg utility, PR etc
 */
class GeneralSeedJobBuilder {

    String release_code
    String gerritBranch
    String gerritRefspec
    String jobName
    Job build(DslFactory dslFactory) {
    if(!(release_code.matches("codereview") && jobName.matches("codereview/(.*)"))){
        dslFactory.folder(release_code)
        dslFactory.job(release_code +"/z-" + release_code+ "-seed") {
            it.description "This job is a seed job for generating " + release_code + "release pipeline. To change the  parameter of the release pipeline, Please go to streams/release_code/env.properties"
            logRotator {
                numToKeep 8
            }
            label("service-node")
            // Adds environment variables to the build.
            parameters {
                // Defines a simple text parameter, where users can enter a string value.
                stringParam("GERRIT_REFSPEC", gerritRefspec, "Parameter passed by Gerrit code review trigger")
                stringParam("GERRIT_BRANCH", gerritBranch, "Parameter passed by Gerrit code review trigger")
            }
            scm {
                // Adds a Git SCM source.
                git {
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
                        name("origin")
                        refspec("\$GERRIT_REFSPEC")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("\$GERRIT_BRANCH")

                }
            }

            steps {
                shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
                dsl {
                    external 'streams/' + release_code + '/*.groovy'
                    additionalClasspath 'src/main/groovy'
                    // Specifies the action to be taken for job that have been removed from DSL scripts.
                    lookupStrategy 'SEED_JOB'

                    removeAction('DELETE')
                    // Specifies the action to be taken for views that have been removed from DSL scripts.
                    removeViewAction('DELETE')
                }
            }
            triggers {
                upstream(jobName, 'SUCCESS')
            }
    }
    }
    }
}
