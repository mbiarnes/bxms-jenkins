package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class ReleasePipelineSeedJobBuilder {

    String release_code
    String ci_properties_file
    String cfg_file
    String ip_makefile
    String product_root_component
    String bpms_deliverable_list_file
    String repo_builder_script

    Job build(DslFactory dslFactory) {
        dslFactory.folder(release_code + "-release-pipeline")
        dslFactory.job(release_code + "-release-pipeline/z-" + release_code + "-release-pipeline-seed") {
            it.description "This job is a seed job for generating " + release_code + "release pipeline. To change the  parameter of the release pipeline, Please go to streams/release_code/env.properties"
            logRotator {
                numToKeep 8
            }
            // Adds environment variables to the build.
            environmentVariables {

                // The name of the product, e.g., bxms64.
                env("RELEASE_CODE", release_code)

                // Release pipeline CI properties file
                env("CI_PROPERTIES_FILE",ci_properties_file)

                // IP project configuration file
                env("IP_CONFIG_FILE", cfg_file)

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

            }
            
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
                upstream('a-master-seed', 'SUCCESS')
            }
        }
    }
}
