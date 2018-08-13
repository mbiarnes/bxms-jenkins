package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS General build job , eg utility, PR etc
 */
class CodeReviewJobBuilder {

    def dirNameRow
    String jobName
    String run_rhdm_mvn_with_pme = '''echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
        export MAVEN_OPTS="-Xms2g -Xmx16g -Dgwt-plugin.localWorkers='3' -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit"
        export M3_HOME=~/bin/maven-3.3.9-prod
        export PATH=$M3_HOME/bin:$PATH
        build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
        mvn -Dversion.override=7.0.0.DM -Dversion.suffix=redhat-\${build_date} \\
            -DdependencyManagement=org.kie.rhba.component.management:rhdm-dependency-management-all:7.0.0.DM-redhat-\${build_date} \\
            -DpropertyManagement=org.kie.rhba.component.management:rhdm-dependency-management-all:7.0.0.DM-redhat-\${build_date} \\
            -s /jboss-prod/m2/bxms-dev-repo-settings.xml  clean install
        '''
    String run_rhpam_mvn_with_pme = '''echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
        export MAVEN_OPTS="-Xms2g -Xmx16g -Dgwt-plugin.localWorkers='3' -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit"
        export M3_HOME=~/bin/maven-3.3.9-prod
        export PATH=$M3_HOME/bin:$PATH
        build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
        mvn -Dversion.override=7.0.0.PAM -Dversion.suffix=redhat-\${build_date} \\
            -DdependencyManagement=org.kie.rhba.component.management:rhpam-dependency-management-all:7.0.0.PAM-redhat-\${build_date} \\
            -DpropertyManagement=org.kie.rhba.component.management:rhpam-dependency-management-all:7.0.0.PAM-redhat-\${build_date} \\
            -s /jboss-prod/m2/bxms-dev-repo-settings.xml  clean install
        '''
    String run_make_mead="""
        # Workaround for variable name conflict between Jenkins and ip-tooling
        unset WORKSPACE

        #changed_cfgs=`git diff --name-only HEAD HEAD~1 | grep -E '(rhpam|rhdm).*\\.cfg'`
        changed_cfgs=`git diff-tree --no-commit-id --name-only -r HEAD| grep -E '(rhpam|rhdm).*\\.cfg'`
        echo \$changed_cfgs
        for cfg in \${changed_cfgs[*]}
        do
            echo "Changes found in \${cfg}, validating..."
            if [[ "\$cfg" =~ -dev ]];then
                build_date=\$(date --date='1 days ago' -u +'%Y%m%d')
                sed -i "s#-SNAPSHOT#-\${build_date}#g" \${cfg}
            fi
            if [[ "\$cfg" =~ ^rhdm ]];then
                product_name="rhdm"
            elif [[ "\$cfg" =~ ^rhpam ]];then
                product_name="rhpam"
            fi
            VALIDATE_ONLY=true LOCAL=1 REPO_GROUP=MEAD+JENKINS+JBOSS+CENTRAL CFG=./\${cfg} MVN_LOCAL_REPO=/jboss-prod/m2/bxms-7.0-nightly POMMANIPEXT=\${product_name}-build-bom make -f Makefile.BRMS \${product_name}-installer
        done
        """
    String run_rhpam_bom_generator="""echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            export M3_HOME=~/bin/maven-3.3.9-prod
            export PATH=\$M3_HOME/bin:\$PATH
            build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
            cd rhpam
            cfg=rhpam-70-dev.cfg
            wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/\$cfg
            wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/ip-bom.cfg
            wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/common.cfg
            if [[ "\$cfg" =~ -dev ]];then
                build_date=\$(date --date='1 days ago' -u +'%Y%m%d')
                sed -i "s#-SNAPSHOT#-\${build_date}#g" \${cfg}
            fi
            mvn -U  -Dcfg=\${cfg} -Dcfg.url.template=file://`pwd`/{0}  \
             -Dmanipulation.disable=true -DprojectMetaSkip=true -DversionSuffixSnapshot=true -Dip.config.sha=\${GERRIT_PATCHSET_REVISION} \
             -Dvictims.updates=offline -B -s /jboss-prod/m2/bxms-dev-repo-settings.xml  install
"""
    String run_rhdm_bom_generator="""echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            export M3_HOME=~/bin/maven-3.3.9-prod
            export PATH=\$M3_HOME/bin:\$PATH
            build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
            cd rhdm
            cfg=rhdm-71-dev.cfg
            wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/\$cfg
            wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/ip-bom.cfg
            wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/common.cfg
            if [[ "\$cfg" =~ -dev ]];then
                build_date=\$(date --date='1 days ago' -u +'%Y%m%d')
                sed -i "s#-SNAPSHOT#-\${build_date}#g" \${cfg}
            fi
            mvn -U  -Dcfg=\${cfg} -Dcfg.url.template=file://`pwd`/{0}  \
             -Dmanipulation.disable=true -DprojectMetaSkip=true -DversionSuffixSnapshot=true -Dip.config.sha=\${GERRIT_PATCHSET_REVISION} \
             -Dvictims.updates=offline -B -s /jboss-prod/m2/bxms-dev-repo-settings.xml  install
"""
    String run_ansible_playbook="""echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
    ansible-playbook --vault-password-file=~/.pass site.yml --extra-vars "keypair_name=ansible-config rsa_pub_path=/home/jenkins/.ssh/id_rsa.pub"
"""
    void create_codereview_job(DslFactory dslFactory, String repoName, String shellScript, String node_label, String jobprefix=""){

        def job=dslFactory.job("codereview/" + jobprefix + repoName.replace('/','-')){
            description("Monitor the code change in Gerrit:" + repoName)

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                stringParam( "GERRIT_REFSPEC", "refs/heads/master",  "Parameter passed by Gerrit code review trigger")

                stringParam( "GERRIT_BRANCH", "master",  "Parameter passed by Gerrit code review trigger")

            }
            label(node_label)
            scm {
                // Adds a Git SCM source.
                git {
                    // Adds a remote.
                    remote {
                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/" + repoName)
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

                    project(repoName, "ant:**")
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
                shell(shellScript)
            }
            // clear workspace
            wrappers {
                preBuildCleanup()
            }

            if(jobName.matches("(.*)codereview/(.*)")){
                println "Detected in codereview:Disable jobs in codereview's codereview & utility."
                disabled()
            }
        }

    }

    void utility_bxms_ci_message_monitor(DslFactory dslFactory, String release_code){
        // This job-DSL createsa a job that monitor the bxms ci message
        def job=dslFactory.job(release_code +"/" +'rhba-ci-message-monitor'){
          description("This DSL generates a job that monitor rhba ci message")

          label("service-node")
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
    }
    void utility_pme_update(DslFactory dslFactory, String release_code){
        def job_d = """This job should create a job automatically check and update jenkins-PME-tool"""
        def job=dslFactory.job(release_code +"/" +'rhpam.pme-update'){
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
            label('volumn-node')
        }
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
                            'pattern' dirName.substring(0, dirName.index("_"))
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
    Job build(DslFactory dslFactory) {
        for (dirName in dirNameRow) {
            switch(dirName) {
                case "codereview":
                    dslFactory.folder(dirName)
                    create_codereview_job(dslFactory,"bxms-licenses-builder", run_rhpam_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"kiegroup/rhap-common",run_rhpam_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"kiegroup/rhdm", run_rhdm_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"kiegroup/rhbas", run_rhpam_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"kiegroup/rhdm-boms", run_rhdm_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"jboss-integration/rhpam-boms", run_rhpam_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"rhdm-maven-repo-root", run_rhdm_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"rhba-maven-repo-root", run_rhpam_mvn_with_pme,"codereview")
                    create_codereview_job(dslFactory,"soa/soa-component-management", run_rhpam_bom_generator,"codereview", "rhpam-")
                    create_codereview_job(dslFactory,"soa/soa-component-management", run_rhdm_bom_generator,"codereview", "rhdm-")
                    create_codereview_job(dslFactory,"rhap-ansible-config", run_ansible_playbook,"service-node-9723")
                    break
                case "utility":
                    dslFactory.folder(dirName)
                    utility_bxms_ci_message_monitor(dslFactory,dirName)
                    utility_pme_update(dslFactory,dirName)
                break
                // default will create the Reviewer's master seed
                default:
                    if(!jobName.matches("(.*)codereview/(.*)")){
                        dslFactory.folder("codereview/"+dirName)
                        createReviwerMasterSeed(dslFactory,dirName)
                    }
                break
            }
        }
    }
}
