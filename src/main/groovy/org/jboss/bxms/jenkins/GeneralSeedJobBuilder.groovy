package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS General build job , eg utility, PR etc
 */
class GeneralSeedJobBuilder {

    String stream_name

    Job build(DslFactory dslFactory) {
        dslFactory.folder(stream_name)
        dslFactory.job(stream_name +"/z-" + stream_name+ "-seed") {
            it.description "This job is a seed job for generating " + stream_name + "release pipeline. To change the  parameter of the release pipeline, Please go to streams/product_name/env.properties"
            logRotator {
                numToKeep 8
            }
            // Adds environment variables to the build.
            scm {
                // Adds a Git SCM source.
                git {
                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("master")
                }
            }

            steps {
                dsl {
                    external 'streams/' + stream_name + '/*.groovy'
                    additionalClasspath 'src/main/groovy'
                    // Specifies the action to be taken for job that have been removed from DSL scripts.
                    lookupStrategy 'SEED_JOB'

                    removeAction('DELETE')
                    // Specifies the action to be taken for views that have been removed from DSL scripts.
                    removeViewAction('DELETE')
                }
            }
            triggers {
                scm 'H/5 * * * *'
            }
            triggers {
                upstream('a-master-seed', 'SUCCESS')
            }
        }
    }
}
