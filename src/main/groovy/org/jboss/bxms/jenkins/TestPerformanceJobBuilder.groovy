package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS General build job , eg utility, PR etc
 */
class TestPerformanceJobBuilder {
    void test_perform_job(DslFactory dslFactory, String repoName, String node_label, int processor, int mem, int gwtworker){
        String run_mvn_with_pme = """echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
        build_date=\$(date -u +'%Y%m%d')

            MVN_LOCAL_REPO=/jboss-prod/m2/bxms-7.0-nightly MVN_SETTINGS=/jboss-prod/m2/bxms-dev-repo-settings.xml \\
            ip-tooling/MEAD_simulator.sh maven-build jb-bxms-7.0-maven-build -m3.3.9-prod \\
            -J-Xms1g -J-Xmx${mem}g -M-T${processor} -Dmaven.test.failure.ignore=true -Dgwt.compiler.localWorkers=${gwtworker} \\
            -M-Drevapi.skip=true -DdependencyManagement=org.kie.rhba.component.management:rhba-dependency-management-all:7.0.0.BA-redhat-\${build_date} \\
            -DenforceSkip=false -Dfull=true -Dmaven.test.failure.ignore=true -DoverrideTransitive=false -Dproductized=true \\
            -DprojectMetaSkip=true -DpropertyManagement= -DrepoReportingRemoval=true -DstrictAlignment=false -DversionOverride=7.7.0 \\
            -DversionSuffix=redhat-${processor}${mem}${gwtworker}-\${build_date} -DversionSuffixSnapshot=true -Dvictims.updates=offline \\
            -Dmaven.local.repo=/jboss-prod/m2/bxms-7.0-nightly git+https://github.com/kiegroup/${repoName}.git#master@2018-02-20,23:55

        """
        def job=dslFactory.job("testPerform/" + repoName.replace('/','-') + "T${processor}-M${mem}-G${gwtworker}"){
            description("Monitor the code change in Gerrit:" + repoName)

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "BRANCH", "master",  "Which branch to test")
            }

            label(node_label)
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

            // build steps
            steps{
                shell(run_mvn_with_pme)
            }
            // clear workspace
            wrappers {
                preBuildCleanup()
            }

        }

    }
    void test_checkout_job_origin(DslFactory dslFactory, String node_label){
        String run_nothing = """echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"        
        """
        def job=dslFactory.job("testPerform/origin-checkout"){
            description("Monitor the code change in Gerrit:")

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "BRANCH", "master",  "Which branch to test")
            }

            label(node_label)
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

            // build steps
            steps{
                shell(run_nothing)
            }
            // clear workspace
            wrappers {
                preBuildCleanup()
            }

        }

    }
    void test_checkout_job_optimize(DslFactory dslFactory, String node_label){
        String bash_checkout = """
        git init .
        git fetch  ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config +refs/heads/master:refs/remotes/origin/master
        git checkout FETCH_HEAD
        mkdir ip-tooling
        cd ip-tooling
        git fetch ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling +refs/heads/master:refs/remotes/origin/master
        git checkout FETCH_HEAD
        cd ..
        mkdir bxms-jenkins
        cd bxms-jenkins
        git init .
        git fetch ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling +refs/heads/master:refs/remotes/origin/master
        git checkout FETCH_HEAD
        cd ..
"""
        String run_nothing = """echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
        
        """
        def job=dslFactory.job("testPerform/optimize-checkout"){
            description("Monitor the code change in Gerrit:" )

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "BRANCH", "master",  "Which branch to test")
            }

            label(node_label)

            // build steps
            steps{
                shell(bash_checkout)
                shell(run_nothing)
            }
            // clear workspace
            wrappers {
                preBuildCleanup()
            }

        }

    }

    Job build(DslFactory dslFactory) {
                    dslFactory.folder("testPerform")
                    test_perform_job(dslFactory,"jbpm", "nightly-node-bigmemory",8,2,1)
                    test_perform_job(dslFactory,"jbpm", "nightly-node-bigmemory",12,2,1)
                    test_perform_job(dslFactory,"jbpm", "nightly-node-bigmemory",14,1,1)

                    test_perform_job(dslFactory,"kie-wb-common", "nightly-node-bigmemory",4,3,2)
                    test_perform_job(dslFactory,"kie-wb-common", "nightly-node-bigmemory",4,2,4)
                    test_perform_job(dslFactory,"kie-wb-common", "nightly-node-bigmemory",6,2,2)
                    test_perform_job(dslFactory,"kie-wb-common", "nightly-node-bigmemory",8,2,6)

                    test_checkout_job_origin(dslFactory,  "nightly-node-bigmemory")
                    test_checkout_job_optimize(dslFactory,"nightly-node-bigmemory")
    }
}
