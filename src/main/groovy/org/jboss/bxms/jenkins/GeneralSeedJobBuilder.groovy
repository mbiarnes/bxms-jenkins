package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS General build job , eg utility, PR etc
 */
class GeneralSeedJobBuilder {

    def dirNameRow
    String jobName

    void pr_bxms_licenses_builder(DslFactory dslFactory, String release_code){
        def shell_script = '''export MAVEN_OPTS="-Xms2g -Xmx16g -Dgwt-plugin.localWorkers='3' -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit"
        export M3_HOME=/jboss-prod/tools/maven-3.3.9-prod
        export PATH=$M3_HOME/bin:$PATH
        build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
        mvn -Dversion.override=7.0.0.DR -Dversion.suffix=redhat-\${build_date} \\
            -DdependencyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
            -DpropertyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
            -s /jboss-prod/m2/bxms-dev-repo-settings.xml  clean install
        '''
        def job=dslFactory.job(release_code +"/" + 'bxms_licenses_builder_codereview'){
            description("Monitor the code change in bxms-licenses-builder")

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "GERRIT_REFSPEC", "refs/heads/master",  "Parameter passed by Gerrit code review trigger")

                stringParam( "GERRIT_BRANCH", "master",  "Parameter passed by Gerrit code review trigger")

            }

            scm {
                // Adds a Git SCM source.
                git {
                    // Adds a remote.
                    remote {
                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-licenses-builder")
                        name("origin")
                        refspec("+refs/heads/*:refs/remotes/origin/* \$GERRIT_REFSPEC")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("\$GERRIT_BRANCH")
                    extensions {
                        choosingStrategy {
                            gerritTrigger()
                        }
                    }
                }
            }
           triggers{
               gerrit{

                   project("bxms-licenses-builder", "ant:**")
                   events {
                       patchsetCreated()
                   }
                   configure { triggers ->
                       triggers   <<  {
                           'serverName' 'code.engineering.redhat.com'
                       }
                   }
               }
           }
           label('nightly-node')

           // build steps
           steps{
               shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
               shell(shell_script)
            }
           // clear workspace
            wrappers {
                preBuildCleanup()
            }
            // Adds post-build actions to the job.
            publishers {
                //Archives artifacts with each build.
                archiveArtifacts('target/*.zip')
            }
        }
        jobCommonEnv(job)
    }
    void pr_ip_config(DslFactory dslFactory, String release_code){
        def shell_script = """
        # Workaround for variable name conflict between Jenkins and ip-tooling
        unset WORKSPACE

        changed_cfgs=`git diff --name-only HEAD HEAD~1 | grep -i ^bxms.\\*\\.cfg`
        for cfg in \${changed_cfgs[*]}
        do
            echo "Changes found in \${cfg}, validating..."
            VALIDATE_ONLY=true LOCAL=1 REPO_GROUP=MEAD+JENKINS+JBOSS+CENTRAL CFG=./\${cfg} MVN_LOCAL_REPO=/jboss-prod/m2/bxms-dev-repo POMMANIPEXT=bxms-bom make -f Makefile.BRMS rhdm-installer rhba-installer
        done
        """

        def job=dslFactory.job(release_code +"/" +'bxms-ip-config-codereview'){
            description("Monitor the code change in integration-platform-config")

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "GERRIT_REFSPEC", "refs/heads/master",  "Parameter passed by Gerrit code review trigger")
                stringParam( "GERRIT_BRANCH", "master",  "Parameter passed by Gerrit code review trigger")

            }
            disabled()
            multiscm {
                // Adds a Git SCM source.
                git {
                    // Adds a remote.
                    remote {
                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                        name("origin")
                        refspec("+refs/heads/*:refs/remotes/origin/* \$GERRIT_REFSPEC")

                    }
                    // Specify the branches to examine for changes and to build.
                    branch("\$GERRIT_BRANCH")
                    extensions {
                        choosingStrategy {
                            gerritTrigger()
                        }

                    }


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
           triggers{
               gerrit{

                   project("integration-platform-config", "ant:**")
                   events {
                       patchsetCreated()
                   }
                   configure { triggers ->
                       triggers   <<  {
                           'serverName' 'code.engineering.redhat.com'
                       }
                       triggers/'gerritProjects'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'/'filePaths'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' << {
                           'compareType' 'REG_EXP'
                           'pattern' 'bxms*.cfg'
                       }
                   }
               }
           }
           // build steps
           steps{
               shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
               shell(shell_script)
            }
           // clear workspace
            wrappers {
                preBuildCleanup()
            }
        }
        jobCommonEnv(job)
    }
    void pr_rhba_common(DslFactory dslFactory, String release_code){
        def shell_script = """wget http://git.app.eng.bos.redhat.com/git/integration-platform-tooling.git/plain/jssecacerts
        export _KEYSTORE=`pwd`/jssecacerts
        export MAVEN_OPTS="-Djavax.net.ssl.trustStore=\${_KEYSTORE} -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.keyStore=\${_KEYSTORE} -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStoreType=jks -Xms512m -Xmx3096m -XX:MaxPermSize=1024m -Dgwt-plugin.localWorkers='3' -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit"
        export M3_HOME=/jboss-prod/tools/maven-3.3.9-prod
        export PATH=\$M3_HOME/bin:\$PATH
        build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
        mvn  -Dversion.override=7.0.0.DR -Dversion.suffix=redhat-\${build_date} \\
            -DdependencyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
            -DversionOverride=true -DversionSuffixSnapshot=true -Dvictims.updates=offline -B -U -s /jboss-prod/m2/bxms-dev-repo-settings.xml clean package
        """
        def job=dslFactory.job(release_code +"/" +'rhba_common_codereview'){
            description("Monitor the code change in rhba-common")

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "GERRIT_REFSPEC", "refs/heads/master",  "Parameter passed by Gerrit code review trigger")

                stringParam( "GERRIT_BRANCH", "master",  "Parameter passed by Gerrit code review trigger")

            }
            scm {
                // Adds a Git SCM source.
                git {
                    // Adds a remote.
                    remote {
                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/kiegroup/rhap-common")
                        name("origin")
                        refspec("+refs/heads/*:refs/remotes/origin/* \$GERRIT_REFSPEC")

                    }
                    // Specify the branches to examine for changes and to build.
                    branch("\$GERRIT_BRANCH")
                    extensions {
                        choosingStrategy {
                            gerritTrigger()
                        }

                    }


                }
            }
           triggers{
               gerrit{

                   project("kiegroup/rhap-common", "master")
                   events {
                       patchsetCreated()
                   }
                   configure { triggers ->
                       triggers   <<  {
                           'serverName' 'code.engineering.redhat.com'
                       }
                   }
               }
           }

           // build steps
           steps{
               shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
               shell(shell_script)
            }
           // clear workspace
            wrappers {
                preBuildCleanup()
            }
            // Adds post-build actions to the job.
            publishers {
                //Archives artifacts with each build.
                archiveArtifacts('target/*.zip')
            }
        }
        jobCommonEnv(job)
    }
    void pr_rhba(DslFactory dslFactory, String release_code){
        def shell_script = """wget http://git.app.eng.bos.redhat.com/git/integration-platform-tooling.git/plain/jssecacerts
        export _KEYSTORE=`pwd`/jssecacerts
        export MAVEN_OPTS="-Djavax.net.ssl.trustStore=\${_KEYSTORE} -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.keyStore=\${_KEYSTORE} -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStoreType=jks -Xms512m -Xmx3096m "
        export M3_HOME=/jboss-prod/tools/maven-3.3.9-prod
        export PATH=\$M3_HOME/bin:\$PATH
        build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
        mvn  -Dversion.override=7.0.0.DR -Dversion.suffix=redhat-\${build_date} -Dversion.suffix.snapshot=true \\
             -DdependencyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
             -DpropertyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
            -DversionOverride=true -DversionSuffixSnapshot=true -Dvictims.updates=offline -B -U -s /jboss-prod/m2/bxms-dev-repo-settings.xml clean package"""
        def job=dslFactory.job(release_code +"/" +'rhbas_codereview'){
            description("Monitor the code change in rhba")

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "GERRIT_REFSPEC", "refs/heads/master",  "Parameter passed by Gerrit code review trigger")

                stringParam( "GERRIT_BRANCH", "master",  "Parameter passed by Gerrit code review trigger")

            }
            scm {
                // Adds a Git SCM source.
                git {
                    // Adds a remote.
                    remote {
                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/kiegroup/rhbas")
                        name("origin")
                        refspec("+refs/heads/*:refs/remotes/origin/* \$GERRIT_REFSPEC")

                    }
                    // Specify the branches to examine for changes and to build.
                    branch("\$GERRIT_BRANCH")
                    extensions {
                        choosingStrategy {
                            gerritTrigger()
                        }

                    }


                }
            }
           triggers{
               gerrit{

                   project("kiegroup/rhbas", "master")
                   events {
                       patchsetCreated()
                   }
                   configure { triggers ->
                       triggers   <<  {
                           'serverName' 'code.engineering.redhat.com'
                       }
                   }
               }
           }

           // build steps
           steps{
               shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
               shell(shell_script)
            }
           // clear workspace
            wrappers {
                preBuildCleanup()
            }
            // Adds post-build actions to the job.
            publishers {
                //Archives artifacts with each build.
                archiveArtifacts('target/*.zip,target/*-standalone.jar')
            }
        }
        jobCommonEnv(job)
    }
    void pr_rhdm(DslFactory dslFactory, String release_code){
        def shell_script = """wget http://git.app.eng.bos.redhat.com/git/integration-platform-tooling.git/plain/jssecacerts
        export _KEYSTORE=`pwd`/jssecacerts
        export MAVEN_OPTS="-Djavax.net.ssl.trustStore=\${_KEYSTORE} -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.keyStore=\${_KEYSTORE} -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStoreType=jks -Xms512m -Xmx3096m -XX:MaxPermSize=1024m"
        export M3_HOME=/jboss-prod/tools/maven-3.3.9-prod
        export PATH=\$M3_HOME/bin:\$PATH
        build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
        mvn  -Dversion.override=7.0.0.DR -Dversion.suffix=redhat-\${build_date} \\
            -DdependencyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
            -DpropertyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
            -DversionOverride=true -DversionSuffixSnapshot=true -Dvictims.updates=offline -B -U -s /jboss-prod/m2/bxms-dev-repo-settings.xml clean package
        """
        def job=dslFactory.job(release_code +"/" +'rhdm_codereview'){
            description("Monitor the code change in rhdm")

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "GERRIT_REFSPEC", "refs/heads/master",  "Parameter passed by Gerrit code review trigger")

                stringParam( "GERRIT_BRANCH", "master",  "Parameter passed by Gerrit code review trigger")

            }
            scm {
                // Adds a Git SCM source.
                git {
                    // Adds a remote.
                    remote {
                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/kiegroup/rhdm")
                        name("origin")
                        refspec("+refs/heads/*:refs/remotes/origin/* \$GERRIT_REFSPEC")

                    }
                    // Specify the branches to examine for changes and to build.
                    branch("\$GERRIT_BRANCH")
                    extensions {
                        choosingStrategy {
                            gerritTrigger()
                        }

                    }


                }
            }
           triggers{
               gerrit{

                   project("kiegroup/rhdm", "master")
                   events {
                       patchsetCreated()
                   }
                   configure { triggers ->
                       triggers   <<  {
                           'serverName' 'code.engineering.redhat.com'
                       }
                   }
               }
           }

           // build steps
           steps{
               shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
               shell(shell_script)
            }
           // clear workspace
            wrappers {
                preBuildCleanup()
            }
            // Adds post-build actions to the job.
            publishers {
                //Archives artifacts with each build.
                archiveArtifacts('target/*.zip,target/*-standalone.jar')
            }
        }
        jobCommonEnv(job)
    }
    void utility_bxms_ci_message_monitor(DslFactory dslFactory, String release_code){
        // This job-DSL createsa a job that monitor the bxms ci message
        def job=dslFactory.job(release_code +"/" +'rhba-ci-message-monitor'){
          description("This DSL generates a job that monitor bxms ci message")

          // both tag and label will trigger the job
          triggers{
            ciBuildTrigger {
                selector("label='rhba-ci'")
                providerName('CI Publish')
            }
          }

          // print message
          steps{
              shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
              shell('''echo "I am trigger by a CI message"
        echo "$CI_MESSAGE"''')
            }

        }
        jobCommonEnv(job)

    }
    void utility_pme_update(DslFactory dslFactory, String release_code){
        def job_d = """This job should create a job automatically check and update jenkins-PME-tool"""
        def job=dslFactory.job(release_code +"/" +'bxms-pme-update'){
           description("$job_d")
           // check daily
           triggers{
                cron('@daily')
           }

           // build steps
           steps{
               shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
                // shell script to check latest version of PME and update accordingly
                        shell('''#!/bin/bash
                echo "Checking maven-metadata.xml ..."
                if ! wget http://download-node-02.eng.bos.redhat.com/brewroot/repos/jb-bxms-6.4-build/latest/maven/org/commonjava/maven/ext/pom-manipulation-ext/maven-metadata.xml;then
                	echo "Download failed"
                    exit 1
                fi
                version=$(grep -oP '(?<=latest>)[^<]+' "./maven-metadata.xml")
                echo "Latest version online is $version"
                echo "In ext:"
                cd /mnt/jboss-prod/tools/maven-extension/
                if [ -f pom-manipulation-ext-$version.jar ];then
                	echo "Find the latest version"
                else
                    echo "Does not find the latest version"
                	echo "Clearing previous version..."
                    find |grep "pom-manipulation-ext"|grep ".jar"| xargs rm -f
                	if wget http://download-node-02.eng.bos.redhat.com/brewroot/repos/jb-bxms-6.4-build/latest/maven/org/commonjava/maven/ext/pom-manipulation-ext/"$version"/pom-manipulation-ext-"$version".jar;then
                	    echo "Latest version has downloaded"
                    else
                        exit 1
                    fi
                fi
                echo "finished"'''
                )
            }
           // clear workspace
            wrappers {
                preBuildCleanup()
            }
        }
        jobCommonEnv(job, 'volumn-node')

    }

    void createReviwerMasterSeed(DslFactory dslFactory,String dirName){
        def job=dslFactory.job("codereview/"+dirName +"/" +dirName+'_master_seed'){
            description('This job controls all codereview jobs generation. It includes the sub seed jobs and pipelines, and execution jobs etc.')
            logRotator {
                numToKeep(5)
                artifactNumToKeep(1)
            }
            parameters {
                stringParam( "GERRIT_REFSPEC", "+refs/heads/master:refs/remotes/origin/master",  "Parameter passed by Gerrit code review trigger")
            }
            label("service-node")
            scm{
                git {
                    remote {
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
                        name("origin")
                        refspec("\$GERRIT_REFSPEC")
                    }
                    branch('FETCH_HEAD')
                }
                // extensions{
                //     wipeOutWorkspace()
                // }
            }
            triggers{
                gerrit{

                    project("bxms-jenkins", "ant:**")
                    events {
                        patchsetCreated()
                    }
                    configure { triggers ->
                        triggers   <<  {
                            'serverName' 'code.engineering.redhat.com'
                        }
                        triggers/'gerritProjects'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'/'topics'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Topic' << {
                            'compareType' 'PLAIN'
                            'pattern' dirName
                        }
                    }
                }
            }
            steps{
                dsl{
                    external("jobs/a_master_seed.groovy")
                    additionalClasspath('''src/main/groovy
                    lib/*.jar''')
                    // Specifies the action to be taken for job that have been removed from DSL scripts.
                    lookupStrategy 'SEED_JOB'

                    removeAction('DELETE')
                    // Specifies the action to be taken for views that have been removed from DSL scripts.
                    removeViewAction('DELETE')
                }
            }

        }
    }

    void jobCommonEnv(Job job, String node_label="nightly-node&&codereview"){
        job.with{
            label(node_label)
            if(jobName.matches("codereview/(.*)")){
                println "Detected in codereview:Disable jobs in codereview's codereview & utility."
                disabled()
            }

        }
    }

    Job build(DslFactory dslFactory) {
        for (dirName in dirNameRow) {
            switch(dirName) {
                case "codereview":
                    dslFactory.folder(dirName)
                    pr_bxms_licenses_builder(dslFactory,dirName)
                    pr_ip_config(dslFactory,dirName)
                    pr_rhba_common(dslFactory,dirName)
                    pr_rhba(dslFactory,dirName)
                    pr_rhdm(dslFactory,dirName)
                break
                case "utility":
                    dslFactory.folder(dirName)
                    utility_bxms_ci_message_monitor(dslFactory,dirName)
                    utility_pme_update(dslFactory,dirName)
                break
                // default will create the Reviewer's master seed
                default:
                    if(!jobName.matches("codereview/(.*)")){
                        dslFactory.folder("codereview/"+dirName)
                        createReviwerMasterSeed(dslFactory,dirName)
                    }
                break
            }
        }
    }
}
