package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class JenkinsAllJobBuilder {
    String release_code
    String ci_properties_file
    String job_type
    String cfg_file

    Job build(DslFactory dslFactory) {

        String _cfg = cfg_file
        Map<String, String> maven_repo_map=[
                "rhdm":"/jboss-prod/m2/bxms-7.0-", \
                "rhba":"/jboss-prod/m2/bxms-7.0-", \
                "rhdm-test":"/jboss-prod/m2/bxms-7.0-"]
        String maven_repo = maven_repo_map [release_code] + job_type

        //Use .m2/repository as local repo
        String shellScript = """
unset WORKSPACE
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
    DEP_REPO=`pwd`/workspace/.m2deploy
    _local_repo=${maven_repo}
if [ "\$LOCAL_REPO" = "true" ];then
    _local_repo=`pwd`/workspace/.m2
else
    _local_repo=${maven_repo}
fi

MVN_DEP_REPO=nexus-release::default::file://\${DEP_REPO} LOCAL=1 CFG=${_cfg} MVN_LOCAL_REPO=\${_local_repo} POMMANIPEXT=\${product_lowercase}-build-bom make -f Makefile.BRMS \${product_lowercase}-installer
"""

        dslFactory.folder(release_code + "-" + job_type + "-release-pipeline")
        dslFactory.job(release_code + "-" + job_type + "-release-pipeline/x-" + release_code + "-all") {
            it.description "This job is a seed job for generating " + release_code + " " +  job_type + " jenkins full build."
            environmentVariables {
                // Adds environment variables from a properties file.
                propertiesFile(ci_properties_file)

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

            }
            logRotator {
                numToKeep 8
            }
            parameters {
                // Defines a simple text parameter, where users can enter a string value.
                booleanParam('LOCAL_REPO', false, 'It will be slower but cleaner since it do not use jenkins cached repo')
                stringParam('CONFIG_REFS','+refs/heads/master:refs/remotes/origin/master','The refs of integration-platform-config you want to pull,defautl master.')
                stringParam('TOOLING_REFS','+refs/heads/master:refs/remotes/origin/master','The refs of integration-platform-tooling you want to pull,defautl master.')
            }
            label("nightly-node-bigmemory")
            multiscm {

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                        refspec("\$CONFIG_REFS")
                    }
                    // Specify the branches to examine for changes and to build.
                    branch("FETCH_HEAD")
                }

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling")
                        refspec("\$TOOLING_REFS")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("FETCH_HEAD")

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
            publishers {
                archiveJunit("**/TEST-*.xml"){
                    allowEmptyResults(true)
                }
                archiveArtifacts{
                    onlyIfSuccessful(false)
                    allowEmpty(true)
                    pattern("**/*.log")
                }
            }
        }
    }
}
