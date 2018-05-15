package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import org.apache.commons.lang.RandomStringUtils
/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class ReleaseSingleJobBuilder {
    String release_code
    String cfg_file
    String ci_properties_file
    String gerritBranch
    String gerritRefspec
    String jobName
    void brewRepoRegen(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "brew-repo-regen"){
            description("This job is responsible for finding brew missing jars.")

            // Adds build steps to the jobs.
            steps {
                shell('echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"')
                ciGenerateBrewRepoBuilder {
                    // Tag name to generate repo for.
                    tag("jb-bxms-7.0-maven-build")
                    // BrewHUB url for Brew operations.
                    brewhuburl("https://brewhub.engineering.redhat.com/brewhub")
                    // Principal name to use for authentication.
                    principal("CI/ci-user.ci-bus.lab.eng.rdu2.redhat.com@REDHAT.COM")
                    // Path name for keytab used in authentication.
                    keytab('${JENKINS_HOME}/plugins/redhat-ci-plugin/ci-user.keytab')
                    // Do not fail the build step if the tagging operation fails.
                    ignore(false)
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
    }
    void monitoringCimessage(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "monitoring-cimessage"){
            def shellScript = """
            # Disable bash tracking mode, too much noise.
            set -x
            kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

            echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            echo \$CI_TYPE
            echo \$CI_NAME
            echo "++++++++++++++++++++++++++++++++++++++"
            #Append the properties into ci properties file
            function appendProp() {
                echo "Inject Properties:\$2"
                if [ -z "\$1" ] || [ -z "\$2" ];then
                    echo "Properties value is empty"
                    exit 1
                fi
                sed -i "/^\$1/d" \${CI_PROPERTIES_FILE} && echo "\$1=\$2" >> \${CI_PROPERTIES_FILE}
            }
            if [ "\$CI_TYPE" = "brew-tag" ];then
                    if [ "\$release_status" != "running" ];then
                            exit 0
                    fi

                    brew_tag_name=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['tag']['name']"` 1>/dev/null
                    if [ "\$brew_tag_name" != "\$brew_target" ];then
                        exit 0
                    fi
                    version=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['version']"` 1>/dev/null
                    release=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['release']"` 1>/dev/null
                    task_id=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['task_id']"` 1>/dev/null
                    nvr=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['nvr']"` 1>/dev/null
                if [ "\$CI_NAME" = "org.kie.rhba-rhpam" ] || [ "\$CI_NAME" = "org.kie.rhba-rhdm" ] ;then
                    appendProp "\${product_lowercase}_assembly_maven_repo_url" "http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
                    appendProp "\${product_lowercase}_assembly_brew_url" "https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
                    appendProp "\${product_lowercase}_assembly_nvr" "\$nvr"
                    #ip-tooling/jira_helper.py -c \${IP_CONFIG_FILE} -a "Product Assembly Build Completed: \${product_lowercase}_assembly_brew_url Build nvr: \${product_lowercase}_assembly_nvr " -f
                elif [ "\$CI_NAME" = "org.kie.rhba-license-builder" ];then
                    license_builder_maven_repo_url=
                    appendProp "license_builder_maven_repo_url" "http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
                elif [ "\$CI_NAME" = "org.jboss.installer-rhdm-installer" ] || [ "\$CI_NAME" = "org.jboss.installer-rhpam-installer" ];then
                    appendProp "\${product_lowercase}_installer_maven_repo_url" "http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
                    appendProp "\${product_lowercase}_installer_brew_url" "https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
                    appendProp "\${product_lowercase}_installer_nvr" "\$nvr"
                    web_hook=`grep "register_web_hook" \${CI_PROPERTIES_FILE} |cut -d "=" -f2`
                    curl -X POST -d 'OK' -k \$web_hook
                    ip-tooling/jira_helper.py -c \${IP_CONFIG_FILE} -a "Brewchain Build Completed, Build nvr: \$nvr " -f
                elif [ "\$CI_NAME" = "org.jboss.brms-bpmsuite.patching-patching-tools-parent" ];then
                    bxms_patch_maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
                    bxms_patch_brew_url="https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
                    bxms_patch_nvr="\$nvr"
                    appendProp "bxms_patch_maven_repo_url" \$bxms_patch_maven_repo_url
                    appendProp "bxms_patch_maven_repo_url" \$bxms_patch_maven_repo_url
                    appendProp "bxms_patch_nvr" \$bxms_patch_nvr
                    #web_hook=`grep "register_web_hook" \${CI_PROPERTIES_FILE} |cut -d "=" -f2`
                    #curl -X POST -d 'OK' -k \$web_hook
                    ip-tooling/jira_helper.py -c \${IP_CONFIG_FILE} -a "Patch Brew Build Completed: \$bxms_patch_brew_url nvr:\$bxms_patch_nvr " -f
                fi
            elif [ "\$label" = "rhba-ci" ];then
                if [ "\$release_status" = "closed" ];then
                    exit 0
                fi
                if [[ "\$EVENT_TYPE" =~ \${product_version_major}\${product_version_minor}-brew-qe-trigger\$ ]];then
                    echo "Triggered by  bxms-prod ci message "
                    echo "\$CI_MESSAGE"
                elif [ "\$EVENT_TYPE" == "\${product_lowercase}-\${product_version_major}\${product_version_minor}-\${release_type}-qe-smoke-results" ];then
                    echo "QE smoketest report:\$CI_MESSAGE"
                    #Json to adoc
                    echo \${CI_MESSAGE}| python -c "import sys, json;
_report=json.load(sys.stdin)
adoc_file=open('\${qe_smoketest_report_path}', 'w')
adoc_file.write('=== QE smoketest Report\\n')
adoc_file.write('[width=100%,options=header,footer,align=center,frame=all]\\n')
adoc_file.write('|============\\n')
adoc_file.write('|Statistics|\\n')
_success=[]
_unsuccess=[]
_statistics=[]
for key in _report:
    if ('SuccessfulJobs' in key):
        for x in _report[key]:
            _success.append(x)
    elif ('UnsuccessfulJobs' in key):
        for x in _report[key]:
            _unsuccess.append(x)
    elif ('Statistics' in key):
        for x in _report[key]:
            adoc_file.write('|' + x + '|' + str(_report[key][x]) +'\\n')

adoc_file.write('|#UNSUCCESSFUL#|')
adoc_file.write('\\n\\n'.join(map(str,_unsuccess)))
adoc_file.write('\\n')
adoc_file.write('|URL|\${qe_smoketest_job_url}\\n')
adoc_file.write('|============\\n')
adoc_file.close()
"
    cat \${qe_smoketest_report_path}
    #web_hook=`grep "register_web_hook" \${CI_PROPERTIES_FILE} |cut -d "=" -f2`
    #curl -X POST -d 'OK' -k \$web_hook
        ip-tooling/jira_helper.py -c \${IP_CONFIG_FILE} -a "QE smoketest returned" -f
    fi
elif [ "\$new" = "FAILED" ] && [ "\$method" = "chainmaven" ];then
    ip-tooling/jira_helper.py -c \${IP_CONFIG_FILE} -a "Brewchain failed: \$brewchain_build_url " -f
    web_hook=`grep "register_web_hook" \${CI_PROPERTIES_FILE} |cut -d "=" -f2`
    curl -X POST -d 'STOP' -k \$web_hook
else
    echo "ERROR!Not triggered by CI!"
    exit 1
fi
"""
            int randomStringLength = 32
            String charset = (('a'..'z') + ('A'..'Z') + ('0'..'9')).join()
            String randomString = RandomStringUtils.random(randomStringLength, charset.toCharArray())
            println "randomString:"+randomString
            // Creates or updates a free style job.
            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            if(jobName.matches("(.*)codereview/(.*)")){
                println "Detected in codereview:Disable monitoring-cimessage"
                disabled()
            }
            triggers{
                ciBuildTrigger {
                    overrides {
                      topic("Consumer.rh-jenkins-ci-plugin.${randomString}.VirtualTopic.qe.ci.>")
                    }
                    selector("label='rhba-ci' OR (CI_TYPE='brew-tag' AND ( CI_NAME='org.kie.rhba-rhdm' OR CI_NAME='org.kie.rhba-rhpam' OR CI_NAME='org.kie.rhba-license-builder' OR CI_NAME='org.jboss.installer-rhdm-installer' OR CI_NAME='org.jboss.installer-rhpam-installer' OR CI_NAME='org.jboss.brms-bpmsuite.patching-patching-tools-parent')) OR (new='FAILED' AND method='chainmaven' AND target='jb-bxms-7.0-maven-candidate')")
                    //providerName('CI Publish')
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void createHandover(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "create-handover"){
            // Create handover script
            def shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            python ip-tooling/template_helper.py -i  ${handover_template_basename}-${product_lowercase}.template -p ${CI_PROPERTIES_FILE} -o  ${release_handover_basename}-${product_lowercase}.adoc
            asciidoctor  ${release_handover_basename}-${product_lowercase}.adoc

            git config --global user.email "jb-ip-tooling-jenkins@redhat.com"
            git config --global user.name "bxms-prod"

            if [ -f  ${product_pvt_report_basename}.html ];then
                cp  ${product_pvt_report_basename}.html  ${archive_pvt_report_basename}-${product_lowercase}.html
            fi

            cd bxms-jenkins
            git add --all

            #sed -i 's/releaseci_trigger=true/releaseci_trigger=false/g' ${CI_PROPERTIES_FILE}
            commit_msg="Prepare handover PR  ${product_name}  ${product_version}  ${product_milestone}"

            git commit -m "${commit_msg}"
            git push origin HEAD:refs/for/master 2>&1| tee b.log

            handover_pr=`grep "${commit_msg}" b.log`
            handover_pr=${handover_pr#remote: }
            handover_pr=${handover_pr%% Prepare*}
            handover_pr=`echo -e "${handover_pr}" | tr -d '[:space:]'`
            #Update the handover pr link
            sed -i '/^handover_pr=/d' ${CI_PROPERTIES_FILE} && echo "handover_pr=$handover_pr" >>${CI_PROPERTIES_FILE}

            echo "JOB DONE"
            '''
            // Sets a description for the job.
            description("This job creates the handover report and pushes it to the staging area.")

            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }

            // Adds post-build actions to the job.
            publishers {

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                        // Adds a target server.
                        verbose(true)

                            // Adds a transfer set.
                            transferSet {

                                // Sets the files to upload to a server.
                                sourceFiles('${archive_pvt_report_basename}-${product_lowercase}.html,${release_handover_basename}-${product_lowercase}.html')

                                // Sets the first part of the file path that should not be created on the remote server.
                                removePrefix('${release_stream_path}/release-history')

                                // Sets the destination folder.
                                remoteDirectory('${product_staging_path}')
                            }
                        }
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void createProductTag(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "create-product-tag"){
            String shellScript = '''
            #unset Jenkins WORKSPACE variable to avoid clash with ip-tooling
            unset WORKSPACE
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            echo -e \"Host code.engineering.redhat.com \\n\\
                    HostName code.engineering.redhat.com \\n\\
                    User jb-ip-tooling-jenkins\" > ~/.ssh/config
            chmod 600 ~/.ssh/config
            MVN_LOCAL_REPO=/jboss-prod/m2/bxms-dev-repo RELEASE_TAG=${product_release_tag} LOCAL=1 CFG=./${IP_CONFIG_FILE} \
                REPO_GROUP=MEAD make POMMANIPEXT=${product_lowercase}-build-bom -f  ${makefile}  ${product_lowercase} 2>&1

            #need to verify if all tags are created succesfully
            EXIST_MISSING_TAG=0
            echo "Verifying  ${product_release_tag} tag..."

            #extract all tag locations from the log file
            cat ${IP_CONFIG_FILE} | grep -Eo "https://code.engineering.redhat.com.*\\.git"| awk -F"/" '{print $5"/"$6}' | grep -Eo ".*\\.git" > tags_location.txt

            while read -r line;do
               # curl the tag url; if find return HTTP/1.1 200 OK; if not,return HTTP/1.1 404 Not found
               curl -Is "http://git.app.eng.bos.redhat.com/git/${line}/tag/?h=${product_release_tag}" | head -n 1 > curl_result
               if grep -q "404" curl_result;then
                  echo "Missing  ${product_release_tag} tag in  ${line}. Please perform some checking..."
                  EXIST_MISSING_TAG=1
               fi
            done < tags_location.txt

            #clear temp files
            rm tags_location.txt
            rm curl_result

            #print result
            if [ ${EXIST_MISSING_TAG} -eq 0 ]; then
               echo "All tags have been successfully found"
            else
               echo "Failed to create some tags"
               exit 1
            fi
            '''
            description("This job is responsible for creating the product milestone tags for this release in the format of ProductVersion.Milestone.")
            label("nightly-node")
            wrappers {
                // Deletes files from the workspace before the build starts.
                preBuildCleanup(){
                    includePattern('workspace/**')
                    deleteDirectories()
                }

            }
            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void releaseNotes(DslFactory dslFactory){
        //TODO remove product2
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "release-notes"){
            def shellScript ='''#!/bin/sh
            echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            product2_jql_cve_search="(project = \${product_name}) AND 'Target Release' = ${product_version}.GA AND labels = security AND (Status = closed or Status = VERIFIED)"
            product2_jql_bugfix_search="(project = \${product_name}) AND 'Target Release' = ${product_version}.GA AND (status = VERIFIED or status = closed) AND summary !~ 'CVE*'"

            product_jql_cve_search="(project = \${product_name}) AND 'Target Release' = ${product_version}.GA AND labels = security AND (status = VERIFIED or status = closed) AND component not in ('Form Modeler', 'jBPM Core', 'jBPM Designer') "
            product_jql_bugfix_search="(project = \${product_name}) AND 'Target Release' = ${product_version}.GA AND (status = VERIFIED or status = closed) AND component not in ('Form Modeler', 'jBPM Core', 'jBPM Designer') AND summary !~ 'CVE*'"

            kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
            function generate_release_notes () {
                #Replace the variable from properties
                python ip-tooling/template_helper.py -i \${release_notes_template} -p \${ci_properties_file} -o \$1

                echo "cve search link: Please click the following link"
                echo "https://issues.jboss.org/issues/?jql=$2"|sed -e "s/ /%20/g"| sed -e "s/'/%22/g"
                echo "bug fix search link: Please click the following link"
                echo "https://issues.jboss.org/issues/?jql=$3"|sed -e "s/ /%20/g"| sed -e "s/'/%22/g"

                #Parsing the JIRA result by awk
                ./ip-tooling/release-ticketor.py --user mw-prod-ci --password ds54sdfs54df --jql "$2" $product_version.GA $cutoff_date $product_version 2>&1 | tee ./jql_search_data.txt
                wc -l ./jql_search_data.txt
                #Clear repeating lines
                cat jql_search_data.txt|awk \'!n[\$0]++\'|tee jql_search_data.txt

                while read -r line; do
                   table_1=$(echo "$line" |awk -F"- " \'{ print "<tr><td><a href=\\\"https://access.redhat.com/security/cve/\"$1\"\\\">"$1"</a></td><td>\" $2\"</td></tr>\\n"}\')
                   sed -i "/<!--table_1_appending_mark-->/ a $table_1" $1
                done < jql_search_data.txt

                ./ip-tooling/release-ticketor.py --user mw-prod-ci --password ds54sdfs54df  --jql "$3" $product_version.GA $cutoff_date $product_version 2>&1 | tee ./jql_search_data.txt
                wc -l ./jql_search_data.txt

                while read -r line; do
                   table_2=$(echo "$line" |awk -F"- " \'{ print "<tr><td><a href=\\\"https://issues.jboss.org/browse/\"$1\"\\\">"$1"</a></td><td>\"$2\"</td></tr>\\n"}\')
                   sed -i "/<!--table_2_appending_mark-->/ a $table_2" $1
                done < ./jql_search_data.txt
            }

            generate_release_notes ${product_release_notes_path} "${product_jql_cve_search}" "${product_jql_bugfix_search}"
            generate_release_notes ${product2_release_notes_path} "${product2_jql_cve_search}" "${product2_jql_bugfix_search}"

            '''
            // Sets a description for the job.
            description("This job is responsible for generating a html release description.")
            disabled()
            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            publishers {

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                        // Adds a target server.
                        verbose(true)

                        // Adds a transfer set.
                        transferSet {

                            // Sets the files to upload to a server.
                            sourceFiles('${product_release_notes_path}')

                            // Sets the destination folder.
                            remoteDirectory('${product_staging_path}/')
                        }
                    }
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void exportOpenshiftImages(DslFactory dslFactory){
        //TODO needs house cleaning
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "export-openshift-images"){
            String shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            mkdir jboss-bpmsuite-${product_version}-openshift
            cd jboss-bpmsuite-${product_version}-openshift

            #download the zip package
            if [ ! -e maven-artifact-handler.py ];
            then
                if ! wget http://git.app.eng.bos.redhat.com/git/integration-platform-tooling.git/plain/maven-artifact-handler.py
                then
                    exit 1;
                fi
            chmod +x maven-artifact-handler.py
            fi

            if [ ! -e download_list.properties ]
            then
                    echo  """#Format G:A::classifier:package_type
            org.kie.rhba:rhpam.:business-central-standalone:jar:rhpam.business-central.standalone.latest.url
            org.kie.rhba:rhpam.:business-central-eap7:zip:rhpam.business-central-eap7.latest.url
            org.kie.rhba:rhpam.:add-ons:zip:rhpam.addons.latest.url
            org.kie.rhba:rhpam.:execution-server-ee7:zip:rhpam.execution-server.ee7.latest.url""" >>download_list.properties
            fi
            maven_repo_url="http://download-node-02.eng.bos.redhat.com/brewroot/repos/${brew_target}/latest/maven/"
            ./maven-artifact-handler.py --version=${product_artifact_version} --override-version ${product_version}${availability} --deliverable download_list.properties --maven-repo ${maven_repo_url} --output BPMS-${product_version}${availability}

            rm -f download_list.properties maven-artifact-handler.py
            if [ $? -ne 0 ]
            then
                echo "Failed to remove files"
                exit 1;
            fi
            if [ ${skipPackage} = "false"  ]
            then

            #Download image config/sources
            wget https://github.com/jboss-openshift/application-templates/archive/bpmsuite-wip.zip;unzip -j bpmsuite-wip.zip */bpmsuite/bpmsuite70-businesscentral-monitoring-with-smartrouter.json */bpmsuite/bpmsuite70-executionserver-postgresql.json */bpmsuite/bpmsuite70-executionserver-externaldb.json -d application-template;rm -f bpmsuite*.zip;
            wget https://github.com/jboss-container-images/jboss-bpmsuite-7-image/archive/${openshift_image_tag}.zip; unzip ${openshift_image_tag}.zip;mv jboss-bpmsuite-7-image-${openshift_image_tag} standalone-image-source; rm -f ${openshift_image_tag}.zip;rm -f standalone-image-source/.gitignore;
            wget https://github.com/jboss-container-images/jboss-bpmsuite-7-openshift-image/archive/${openshift_image_tag}.zip; unzip ${openshift_image_tag}.zip;mv jboss-bpmsuite-7-openshift-image-${openshift_image_tag} openshift-image-source;rm -f ${openshift_image_tag}.zip;rm -f openshift-image-source/.gitignore;

            if ! wget http://git.app.eng.bos.redhat.com/git/integration-platform-config.git/plain/brms-release/bpmsuite-image-stream.json -P application-template/
            then
            exit 1;
            fi
            sed -i "s/replace_image_version/${openshift_image_version}/g" application-template/bpmsuite-image-stream.json

            #Clean  docker images
            #for i in $(docker images -q);do docker rmi $i; done

            #Define the internal docker registry
            #docker_registry=docker-registry.engineering.redhat.com

            #docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${openshift_image_version}
            #docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-openshift:${openshift_image_version} >bpmsuite70-businesscentral-openshift-${openshift_image_version}.tar
            #docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${openshift_image_version}
            #docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-executionserver-openshift:${openshift_image_version} >bpmsuite70-executionserver-openshift:${openshift_image_version}.tar
            #docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${openshift_image_version}
            #docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-standalonecontroller-openshift:${openshift_image_version} >bpmsuite70-standalonecontroller-openshift-${openshift_image_version}.tar
            #docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${openshift_image_version}
            #docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-smartrouter-openshift:${openshift_image_version} >bpmsuite70-smartrouter-openshift-${openshift_image_version}.tar
            #docker pull ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-monitoring-openshift:${openshift_image_version}
            #docker save ${docker_registry}/jboss-bpmsuite-7/bpmsuite70-businesscentral-monitoring-openshift:${openshift_image_version} >bpmsuite70-businesscentral-monitoring-openshift-${openshift_image_version}.tar

            cd ..
            zip -5 -r  jboss-bpmsuite-${product_version}${availability}-openshift.zip jboss-bpmsuite-${product_version}-openshift/
            md5sum jboss-bpmsuite-${product_version}${availability}-openshift.zip >jboss-bpmsuite-${product_version}${availability}-openshift.zip.md5
            fi
            '''
            // Sets a description for the job.
            description("This job is responsible for exporting openshift images to zip files.")

            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                booleanParam("skipPackage", false ,"Skip package Openshift Image")
            }
            disabled()

            // Adds pre/post actions to the job.
            wrappers {

                // Deletes files from the workspace before the build starts.
                preBuildCleanup()
            }
            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }

            // Adds post-build actions to the job.
            publishers {


            }
            // Adds post-build actions to the job.
            publishers {
                //Archives artifacts with each build.
                archiveArtifacts('**/*.zip,**/*-smart-router.jar')

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                        // Adds a target server.
                        verbose(true)

                        // Adds a transfer set.
                        transferSet {

                            // Sets the files to upload to a server.
                            sourceFiles('jboss-bpmsuite-${product_version}${availability}-openshift.zip*')

                            // Sets the destination folder.
                            remoteDirectory('${product2_staging_path}/')
                        }

                    }
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
    }
    void generateQeProperties(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "generate-qe-properties"){
            def shellScript = '''
            #set -x
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            #kinit -k -t  ${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

            function appendProp(){
                if [ -z "$1" ] || [ -z "$2" ];then
                    echo "Param  is not allow empty"
                    exit 1
                fi
                sed -i "/^$1/d"  ${product_staging_properties_name} && echo "$1=$2" >>  ${product_staging_properties_name}
            }

            if [ "${release_type}" == "nightly" ]; then
                product_url_prefix="${jenkins_cache_url}/${jenkins_cache_repo}/org/kie/rhba/${product_lowercase}/${product_artifact_version}"
                product_installer_url="${jenkins_cache_url}/${jenkins_cache_repo}/org/jboss/installer/${product_lowercase}-installer/${product_artifact_version}/${product_lowercase}-installer-${product_artifact_version}.jar"
                product_filename_common_prefix="${product_lowercase}-${product_artifact_version}"
                product_installer_name="${product_lowercase}-installer-${product_artifact_version}.jar"

            else
                product_url_prefix="${rcm_staging_base}/${product_staging_path}"
                product_filename_common_prefix="${product_lowercase}-${product_upload_version}"
                product_installer_url="${product_url_prefix}/${product_lowercase}-installer-${product_upload_version}.jar"

            fi
            if [ ! -f $product_staging_properties_name ]; then
                touch $product_staging_properties_name
            fi
            case "${product_lowercase}" in
                rhdm )
                    appendProp "${product_lowercase}.decision-central.standalone.latest.url"    "$product_url_prefix/${product_filename_common_prefix}-decision-central-standalone.jar"
                    appendProp "${product_lowercase}.decision-central-eap7.latest.url"          "$product_url_prefix/${product_filename_common_prefix}-decision-central-eap7-deployable.zip"
                    ;;
                rhpam )
                    appendProp "${product_lowercase}.business-central.standalone.latest.url"    "$product_url_prefix/${product_filename_common_prefix}-business-central-standalone.jar"
                    appendProp "${product_lowercase}.business-central-eap7.latest.url"          "$product_url_prefix/${product_filename_common_prefix}-business-central-eap7-deployable.zip"
            esac

            appendProp "${product_lowercase}.kie-server.ee7.latest.url" "${product_url_prefix}/${product_filename_common_prefix}-kie-server-ee7.zip"
            appendProp "${product_lowercase}.addons.latest.url"         "${product_url_prefix}/${product_filename_common_prefix}-add-ons.zip"
            appendProp "${product_lowercase}.installer.latest.url"         "${product_installer_url}"


            appendProp "${product_lowercase^^}_VERSION"   ${product_artifact_version}
            appendProp "KIE_VERSION"                ${kie_version}
            appendProp "APPFORMER_VERSION"          ${appformer_version}
            appendProp "ERRAI_VERSION"              ${errai_version}
            appendProp "MVEL_VERSION"               ${mvel_version}
            appendProp "IZPACK_VERSION"             ${izpack_version}
            appendProp "INSTALLER_COMMONS_VERSION"  ${installercommons_version}
            appendProp "JAVAPARSER_VERSION"         ${drlx_parser_version}

            #Additional properties for brew release
            if [ "${release_type}" == "brew" ]; then
                #append the other properties per qe's requirement
                appendProp "build.config" ${product_url_prefix}/${IP_CONFIG_FILE}
                appendProp ${product_lowercase^^}_PUBLIC_VERSION ${product_upload_version}
                appendProp "${product_lowercase}.maven.repo.latest.url"     "$product_url_prefix/${product_filename_common_prefix}-maven-repository.zip"
                appendProp "${product_lowercase}.sources.latest.url"   "$product_url_prefix/${product_sources_name}"

                sed -e "s=${rcm_staging_base}/${product_lowercase}=${rcm_candidate_base}/${product_lowercase}=g" \
                ${product_staging_properties_name} > ${product_candidate_properties_name}
            fi

            '''
            // Sets a description for the job.
            description("This job is responsible for staging the Brew release deliverable to the RCM staging area.")

            // Adds build steps to the jobs.
            steps {
                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            wrappers {
                // Deletes files from the workspace before the build starts.
                preBuildCleanup()

            }
            publishers {
                publishOverSsh {
                    server('publish server') {
                        // Adds a target server.
                        verbose(true)
                        // Adds a transfer set.
                        transferSet {
                            // Sets the files to upload to a server.
                            sourceFiles('${IP_CONFIG_FILE}, ${product_staging_properties_name}, ${product_candidate_properties_name}')

                            // Sets the destination folder.
                            remoteDirectory('${product_staging_path}')
                        }
                    }
                }
            }

        }
        buildEnv(job)
        buildCommon(job)
    }
    void generateSources(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "generate-sources"){
            def shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            # Kerberos authentication
            kinit -k -t ${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

            # Workaround for variable name conflict between Jenkins and ip-tooling
            unset WORKSPACE

            # Make sources
            make CFG=${IP_CONFIG_FILE} SOURCES=1 POMMANIPEXT=${product_lowercase}-build-bom SRCDIR=src -f Makefile.BRMS ${product_lowercase}-installer

            ## Prepare sources for delivery ##
            cd workspace

            rm -rf src/*-license-${product_artifact_version} \
                   src/license-builder-${product_artifact_version} \
                   src/${product_lowercase}-maven-repo-root-${product_artifact_version} \
                   src/errai-parent-${errai_version} \
                   src/${product_lowercase}\
                   src/kie-parent-${kie_version}/RELEASE-README.md \
                   src/lienzo-tests-${kie_version}

            # Create sources archive
            zip -r -5 --quiet ${product_sources_name} src/
            '''
            // Sets a description for the job.
            description("This job is responsible for generating product sources.")

            // Adds pre/post actions to the job.
            wrappers {
                        // Deletes files from the workspace before the build starts.
                        preBuildCleanup()
                    }

            // Adds build steps to the jobs.
            steps {
                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
                // Inject environment variables for staging paths
            }

            // Adds post-build actions to the job.
            publishers {
                //Archives artifacts with each build.
                archiveArtifacts('workspace/${product_sources_name}')

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                        // Adds a target server.
                        verbose(true)

                        // Adds a transfer set.
                        transferSet {

                            // Sets the files to upload to a server.
                            sourceFiles('workspace/${product_sources_name}')

                            // Sets the first part of the file path that should not be created on the remote server.
                            removePrefix('workspace/')

                            // Sets the destination path.
                            remoteDirectory('${product_staging_path}')
                        }
                    }
                }
            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void initRelease(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "init-release"){
            String shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            remote_product_cfg_sha=$(git log -1 --pretty="%H" ${IP_CONFIG_FILE})
            cfg=${IP_CONFIG_FILE}

            function appendProp() {
                echo "Inject Properties:$1 Value:$2"
                if [ -z "$1" ] ;then
                    echo "Properties name is empty"
                    exit 1
                fi
                sed -i "/^$1/d" ${CI_PROPERTIES_FILE} && echo "$1=$2" >> ${CI_PROPERTIES_FILE}
            }
            if [ "${CLEAN_CONFIG}" = "true" ];then
                rm -vf ${CI_PROPERTIES_FILE}
            fi

            #If build new versions, then remove the jenkins properties files
            if [ -f ${CI_PROPERTIES_FILE} ];then
                new_version="`grep 'product_version=' ${IP_CONFIG_FILE}`"
                new_milestone="`grep 'product_milestone=' ${IP_CONFIG_FILE}`"
                old_version="`grep 'product_version=' ${CI_PROPERTIES_FILE}`"
                old_milestone="`grep 'product_milestone=' ${CI_PROPERTIES_FILE}`"

                export `grep "product_cfg_sha" ${CI_PROPERTIES_FILE}`

                echo "local:$product_cfg_sha, remote: $remote_product_cfg_sha"
                if [ "${new_version}${new_milestone}" != "${old_version}${old_milestone}" ] || \
                   [ "${product_cfg_sha}" != "${remote_product_cfg_sha}" ]
                then
                    rm -vf ${CI_PROPERTIES_FILE}
                fi
            fi
            build_date=$(date -u +'%Y%m%d')
            #Override nightly build properties name
            if [ ! -f ${CI_PROPERTIES_FILE} ];then
                #Loading env from cfg file
                if [[ "${RELEASE_CODE}" =~ nightly ]];then
                    sed -i "s#-SNAPSHOT#-${build_date}#g" rh*-dev.cfg
                fi
                python ip-tooling/jenkins_ci_property_loader.py -m bxms-jenkins/config/properties-mapping.template -i ${IP_CONFIG_FILE} -o ${CI_PROPERTIES_FILE}
                appendProp "product_cfg_sha" $remote_product_cfg_sha
                appendProp "ci_properties_file" ${CI_PROPERTIES_FILE}
                appendProp "build_cfg" ${IP_CONFIG_FILE}
            fi
            source ${CI_PROPERTIES_FILE}

            #Use kerbose to create the release JIRA
            kinit -k -t  ${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
            ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -t  ${release_estimation}  ${release_estimation} -f |tee /tmp/jira.log
            jira_id=`tail -n 2 /tmp/jira.log |head -n 1`
            jira_id=${jira_id/Selected Result:/}
            echo "https://projects.engineering.redhat.com/browse/$jira_id"
            appendProp "release_jira_id" $jira_id
            #build_date is used in nightly build
            appendProp "build_date" "${build_date}"

            appendProp "archive_pvt_report_basename" "bxms-jenkins/streams/${RELEASE_CODE}/release-history/${RELEASE_CODE}-pvt-report"
            appendProp "release_handover_basename" "bxms-jenkins/streams/${RELEASE_CODE}/release-history/release-handover"
            appendProp "release_stream_path" "bxms-jenkins/streams/${RELEASE_CODE}"
            if [ "${release_type}" == "nightly" ];then
                appendProp "product_staging_properties_name" "${product_lowercase}-${build_date}.properties"
                appendProp "product_staging_path" "${product_lowercase}/${product_name}-${product_version}.NIGHTLY"
                appendProp "product_staging_properties_url" "${rcm_staging_base}/${product_lowercase}/${product_name}-${product_version}.NIGHTLY/${product_lowercase}-${build_date}.properties"
            fi
            '''
            // Sets a description for the job.
            description("This is the \${RELEASE_CODE} release initialization job. This job is responsible for preparation of \${CI_PROPERTIES_FILE} file.")
            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                booleanParam( "CLEAN_CONFIG", false, "WARNING, click this will force remove your release pipeline properties!")
            }
            // Label which specifies which nodes this job can run on.
            label("release-pipeline")

            // Adds pre/post actions to the job.

            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            // Adds post-build actions to the job.

        }
        buildEnv(job)
        buildScm(job)
    }
    void locateImportList(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "locate-import-list"){
            String shellScript = '''
            echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            function getGroupId()
            {
              local _input=\$1
              if echo \$_input |grep ".*:.*:.*:.*" 2>&1 1>/dev/null ;then
                echo \$_input |sed "s/\\(.*\\):\\(.*\\):\\(.*\\):\\(.*\\)/\\1/g"
                return 0
              fi

              if echo \$_input |grep ".*:.*:.*" 2>&1 1>/dev/null ;then
                echo \$_input |sed "s/\\(.*\\):\\(.*\\):\\(.*\\)/\\1/g"
                return 0
              fi

              if echo \$_input |grep ".*:.*" 2>&1 1>/dev/null ;then
                echo \$_input |sed "s/\\(.*\\):\\(.*\\)/\\1/g"
                return 0
              fi

              if echo \$_input |grep ".*-.*-.*-.*" 2>&1 1>/dev/null ;then
                local BUILDINFO=`brew buildinfo \$_input`
                echo "\$BUILDINFO" | awk '/Maven groupId:/ {print \$3}'
              fi

            }

            function getArtifactId()
            {
              if echo \$1 |grep ".*:.*:.*:.*" 2>&1 1>/dev/null ;then
                echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\):\\(.*\\)/\\2/g"
                return 0
              fi

              if echo \$1 |grep ".*:.*:.*" 2>&1 1>/dev/null ;then
                echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\)/\\2/g"
                return 0
              fi

              if echo \$1 |grep ".*:.*" 2>&1 1>/dev/null ;then
                echo \$1 |sed "s/\\(.*\\):\\(.*\\)/\\2/g"
                return 0
              fi

              if echo \$1 |grep ".*-.*-.*-.*" 2>&1 1>/dev/null ;then
                local BUILDINFO=`brew buildinfo \$1`
                echo "\$BUILDINFO" | awk '/Maven artifactId:/ {print \$3}'
              fi
            }

            function getVersion()
            {
              if echo \$1 |grep ".*:.*:.*:.*" 2>&1 1>/dev/null ;then
                echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\):\\(.*\\)/\\4/g"
                return 0
              fi

              if echo \$1 |grep ".*:.*:.*" 2>&1 1>/dev/null ;then
                echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\)/\\3/g"
                return 0
              fi

              if echo \$1 |grep ".*-.*-.*-.*" 2>&1 1>/dev/null ;then
                local BUILDINFO=`brew buildinfo \$1`
                echo "\$BUILDINFO" | awk '/Maven version:/ {print \$3}'
              fi
            }

            function importToMead()
            {
                if [ ! -e /tmp/utility-scripts ] ;then
                    echo Checking out utility-scripts...
                    UTILITY_URL="git://git.app.eng.bos.redhat.com/rcm/utility-scripts.git"
                    git clone \$UTILITY_URL /tmp/utility-scripts &> /dev/null
                fi
                PATH=/tmp/utility-scripts/mead:/tmp/utility-scripts:\$PATH

                local _artifact="\$1"
                while [ -n "\$1" ] ;do
                    local _artifact="\$(echo \$1 |tr -d ',')"
                    shift
                    local g=\$(getGroupId \$_artifact)
                    local a=\$(getArtifactId \$_artifact)
                    local v=\$(getVersion \$_artifact)
                    local gavpath=\$(echo \$(echo "\$g" | sed "s|\\.|/|g")/\$a/\$v)
                    local _mavenrepo="/jboss-prod/m2/\${jenkins_cache_repo}"
                    local _importtag=\$brew_importtag
                    local _importowner="bxms-release/prod-ci"
                    echo ":) Importing \$g:\$a:\$v into \$_importtag by \$_importowner"
                    if [ "\$CLEAN_MISSING_ARTIFACT" == "true" ];then
                        rm -rfv \$_mavenrepo/\$gavpath
                        continue
                    fi

                    import-maven --owner=\$_importowner --tag=\$_importtag \$(find \$_mavenrepo/\$gavpath  -name '*.jar' -o -name '*.pom')||true
                    if [ \$? -ne 0 ] ;then
                        mkdir -p \$_mavenrepo/\$gavpath
                        cd \$_mavenrepo/\$gavpath
                        get-maven-artifacts \$g:\$a:\$v
                        import-maven --owner=\$_importowner --tag=\$_importtag *
                        if [ \$? -ne 0 ] ;then
                            echo ":| Failed to import \$_artifact"
                        fi
                    fi
                done
                return 0
            }

            function importToMeadFromLog()
            {
                [[ ! -f \$1 ]] && echo "\$1 is not existed!" && exit 1

                local importlist="\$(grep "MISSING: .*:.*:.* FROM" \$1 | sed 's/MISSING: \\([^: ]*:[^: ]*:[^: ]*\\) .*/\\1/')"
                for i in \$importlist ;do
                importToMead \$i
                if [ \$? -ne 0 ] ;then
                    echo ":( ERROR Failed to import \$i"
                fi
                done
                echo ""
            }

            kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
            echo "Scanning missing artifact..."
            ip-tooling/MEAD_check_artifact.sh \$brew_tag /jboss-prod/m2/\${jenkins_cache_repo} 2>&1 | tee /tmp/mead_check.log
            # echo "`tail -n 5 /tmp/mead_check.log`" > /tmp/mead_check.log # For debug purpose
            sed -ni "/MISSING/p" /tmp/mead_check.log
            sed -i -e "/redhat-/d" -e "/SNAPSHOT/d" -e "/Unknown DIR/d" /tmp/mead_check.log

            importToMeadFromLog /tmp/mead_check.log
            echo "JOB DONE"
            '''
            // Sets a description for the job.
            description("This job is responsible for finding brew missing jars.")
            parameters{
                booleanParam( "CLEAN_MISSING_ARTIFACT", false,"Tick if you want to remove the missing ones. This is useful when the missing report isn't correct for some reason!")
            }
            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)

            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void mavenRepositoryBuild(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "maven-repository-build"){
            def incrementalRepositoryString = null

            //if (RELEASE_CODE == "bxms64") {
            //    incrementalRepositoryString = "http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-brms/BRMS-6.4.0.CR2/jboss-brms-bpmsuite-6.4.0.GA-maven-repository/maven-repository"
            //}
            // Repository builder script
            def shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            kinit -k -t  ${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
            ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Maven repository build started: Build url: ${BUILD_URL}" -f

            PROJECT_NAME=${product_lowercase} make CFG=${IP_CONFIG_FILE} BUILDER_SCRIPT=${repository_builder_script} -f ${makefile} repository
            rename jboss-${product_lowercase} ${product_lowercase} workspace/${product_lowercase}-repository/archive/*
            '''
            // Sets a description for the job.
            description("This job is responsible for building the offline maven repository zip for MRRC.")

            // Allows to parameterize the job.
            parameters {

                // Defines a simple boolean parameter.
                booleanParam("CLEAN_CARTOGRAPHER_CACHE",  false,"Tick if you want to wipe local Cartographer cache to send over new requests.")

                // Defines a simple boolean parameter.
                booleanParam( "DELETE_CARTOGRAPHER_WORKSPACE",  false,"Tick if you want to wipe remote Cartographer workspace containing any resolved dependency graph.")

                // Defines a simple boolean parameter.
                booleanParam( "GEN_REPORT", true,"Tick if you want to generate report for the newly created repository.")

                // Defines a simple text parameter, where users can enter a string value.
                stringParam("INCREMENTAL_REPO_FOR", incrementalRepositoryString, "List of repositories to exclude. They can be online repository urls or online available zip files in format <url to the zip>:<relative path to repo root inside the zip<. Each repository is supposed to be put on a new line.")
            }

            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
                // Inject environment variables for $product_staging_path
            }

            wrappers {
                // Deletes files from the workspace before the build starts.
                preBuildCleanup(){
                    includePattern('workspace/**')
                    deleteDirectories()
                }

            }
            // Adds post-build actions to the job.
            publishers {

                //Archives artifacts with each build.
                archiveArtifacts('workspace/${product_lowercase}-repository/archive/**/*')

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                        // Adds a target server.
                        verbose(true)

                        // Adds a transfer set.
                            transferSet {

                                // Sets the files to upload to a server.
                                sourceFiles('workspace/${product_lowercase}-repository/archive/*.zip,workspace/${product_lowercase}-repository/archive/*.text,workspace/${product_lowercase}-repository/archive/*.md5')

                                // Sets the first part of the file path that should not be created on the remote server.
                                removePrefix('workspace/${product_lowercase}-repository/archive/')

                                // Sets the destination folder.
                                remoteDirectory('${product_staging_path}')

                                // Specifies a command to execute on the remote server.
                                execCommand('unzip ' +
                                        '-o ~/staging/${product_staging_path}/maven-repository-report.zip ' +
                                        '-d ~/staging/${product_staging_path}' +
                                        '&& rm ' +
                                        '-f ~/staging/${product_staging_path}/maven-repository-report.zip')
                            }

                        }
                }
            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void mockQeSmoketestReport(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "mock-qe-smoketest-report"){
            def shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            echo "Done"
            '''
            def report_string = '''{"SuccessfulJobs":{"BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-db":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-wls":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-dashbuilder-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-dashbuilder-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-integration-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-quickstarts-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-united-exec-servers-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-wb-rest-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-wb-rest-smoke-wls":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-bre-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-business-central-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-business-central-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-business-central-smoke-wls":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-quickstarts-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-wb-rest-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bxms-prod-6.4-blessed-inc-maven-repo-testsuite-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bxms-prod-6.4-blessed-maven-repo-testsuite-smoke":"SUCCESS"},"UnsuccessfulJobs":{"BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-clustering-smoke":"FAILURE","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-dashbuilder-smoke-db":"UNSTABLE","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-exec-server-smoke-container":"FAILURE","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-wb-rest-smoke-container":"FAILURE"},"Statistics":{"TotaBuildRuns":23,"SuccessfulBuilds":19,"UnsuccessfulBuilds":4}}'''
                                // Sets a description for the job.
            description("This job is responsible for mocking a CI message triggered returned the smoketest result from QE.")

            // Adds build steps to the jobs.
            steps {
                shell(shellScript)

                // Sends JMS message.
                ciMessageBuilder {
                    overrides {
                        topic('VirtualTopic.qe.ci.ba.${product_lowercase}.${product_version_major}${product_version_minor}.${release_type}.smoke.results')
                    }

                    // JMS selector to choose messages that will fire the trigger.
                    providerName("Red Hat UMB")

                    // Type of CI message to be sent.
                    messageType("Custom")

                    // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
                    messageProperties('label=rhba-ci\n' +
                            'CI_TYPE=customer\n' +
                            'EVENT_TYPE=${product_lowercase}-70-${release_type}-qe-smoke-results\n')

                    // Content of CI message to be sent.
                    messageContent(report_string)
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void openshiftTest(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "openshift-test"){
            String shellScript = '''
            product_lowercase="rhdm"
            echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            sudo oc cluster down || /bin/true
            echo -e "\\n-------\\n$(sudo oc cluster status)\\n--------\\n"
            echo -e "\\n-------\\nnode public-ip:${OPENSTACK_PUBLIC_IP}\\n--------\\n"
            sudo oc cluster up --public-hostname="\${product_lowercase}.usersys.redhat.com" --routing-suffix="\${product_lowercase}.usersys.redhat.com"
            oc login localhost:8443 -u developer -p developer --insecure-skip-tls-verify=true
            oc new-project "${OCPROJECTNAME}" || /bin/true
            oc project "${OCPROJECTNAME}"
            oc policy add-role-to-user admin admin -n "${OCPROJECTNAME}"
            oc policy add-role-to-user admin system:serviceaccount:decisioncentral-service-account:default -n "${OCPROJECTNAME}"
            oc policy add-role-to-user view system:serviceaccount:kieserver-service-account:default -n "${OCPROJECTNAME}"
            oc create -f kieserver-app-secret.yaml
            oc create -f decisioncentral-app-secret.yaml
            #wget image-streams is for fixing the imagestream bug, this won't be needed after my PR is merged
            wget https://raw.githubusercontent.com/ryanzhang/rhdm-7-openshift-image/a8f10a7f4a33dfc3becea3e5e3d7d7107ded109b/rhdm70-image-streams.yaml -O rhdm70-image-streams.yaml

            #Replace the docker image stream from rhcc to brew stage registry since ER image only  availabe in staging folder
            sed -i 's/registry.access.redhat.com/brew-pulp-docker01.web.prod.ext.phx2.redhat.com:8888/g' rhdm70-image-streams.yaml
            oc create -f rhdm70-image-streams.yaml

            oc process -n "${OCPROJECTNAME}" -f templates/rhdm70-full.yaml -p IMAGE_STREAM_NAMESPACE="${OCPROJECTNAME}" -p ADMIN_PASSWORD=admin\\! -p KIE_ADMIN_USER=adminUser -p KIE_ADMIN_PWD=admin1\\! -p KIE_SERVER_CONTROLLER_USER=controllerUser -p KIE_SERVER_CONTROLLER_PWD=controller1\\! -p KIE_SERVER_USER=executionUser -p KIE_SERVER_PWD=execution1\\! |oc create -n "${OCPROJECTNAME}" -f -
            oc expose svc/myapp-rhdmcentr --hostname="\${product_lowercase}.usersys.redhat.com" --name "\${product_lowercase}"
            '''

            // Sets a description for the job.
            description("This job is responsible for test openshift images.")
            scm {

                github("jboss-container-images/rhdm-7-openshift-image")

            }
            parameters{
                stringParam('OCPROJECTNAME', 'myproject', 'the project name to build in openshift')
            }
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            label("openshift-test")

        }
    }

    void promoteRelease(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "promote-release"){
            String shellScript = '''
            echo -e "Exec node IP:${OPENSTACK_PUBLIC_IP}\\n"
            #sed -i '/^release_status=/d' ${CI_PROPERTIES_FILE} && echo "release_status=closed" >>${CI_PROPERTIES_FILE}
            '''
            // Sets a description for the job.
            description("This job is responsible for uploading release to candidate area.")
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            if(jobName.matches("(.*)codereview/(.*)")){
                println "Detected in codereview:Disable promote-release"
                disabled()
            }

            //
            triggers{
                gerrit{

                    project("bxms-jenkins", "ant:**")
                    events {
                        changeMerged()
                    }
                    configure { triggers ->
                        triggers   <<  {
                            'serverName' 'code.engineering.redhat.com'
                        }
                        triggers/'gerritProjects'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'/'filePaths'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' << {
                            'compareType' 'REG_EXP'
                            'pattern' 'stream/rhdm/release-history/*-handover.adoc'
                        }
                    }
                }
            }
            // Adds post-build actions to the job.
            publishers {

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                        // Adds a target server.
                        verbose(true)

                        // Adds a transfer set.
                        transferSet {

                            // Sets a timeout in milliseconds for the command to execute. Defaults to two minutes.
                            execTimeout(0)

                            // Specifies a command to execute on the remote server.
                            execCommand('kinit -k -t ~/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM\n' +
                                    '/mnt/redhat/scripts/rel-eng/utility/bus-clients/stage-mw-release ${product_name}-${product_milestone_version}\n')
                        }
                    }
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
    }
    void pvtTest(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "pvt-test"){
            String shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            git clone https://github.com/project-ncl/pvt.git
            cd pvt
            ~/bin/maven-3.3.9-prod/bin/mvn -Dmaven.repo.local=${dev_maven_repo} \
                surefire-report:report -B -Dproduct.config=${product_smoketest_cfg} -Dproduct.version=${product_milestone_version} \
                -Dproduct.target=${product_upload_version} -Dreport.filepath=${product_pvt_report_basename} clean package

            '''
            // Sets a description for the job.
            description("This job is responsible for executing product validation tests.")

            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            wrappers {
                // Deletes files from the workspace before the build starts.
                preBuildCleanup()

            }
            // Adds post-build actions to the job.

        }
        buildEnv(job)
        buildCommon(job, "nightly-node")
        buildScm(job)
    }
    void sendHandoverMail(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "send-handover-mail"){
            def mailContent = '''Hello,

            $product ${product_milestone_version} is now available, the handover can be found below:

            $rcm_candidate_base/$product_name/$product_name-${product_milestone_version}/${product_lowercase}-handover.html

            $product2 ${product2_milestone_version} is now available, the handover can be found below:

            $rcm_candidate_base/$product2_name/$product2_name-${product2_milestone_version}/${product2_lowcase}-handover.html

            Kind regards,

            BxMS Prod Team'''
            // Sets a description for the job.
            description("This job is responsible for sending handover email to the team.")

            // Adds post-build actions to the job.
            publishers {

                // Sends customizable email notifications.
                extendedEmail {

                    // Adds email addresses that should receive emails.
                    recipientList('pszubiak@redhat.com')

                    // Adds e-mail addresses to use in the Reply-To header of the email.
                    replyToList('bxms-prod@redhat.com')

                    // Sets the default email subject that will be used for each email that is sent.
                    defaultSubject('${product_name}${product_version}${product_milestone} is now available.')

                    // Sets the default email content that will be used for each email that is sent.
                    defaultContent(mailContent)

                    // Sets the content type of the emails sent after a build.
                    contentType('text/html')
                }
            }
            steps {
                shell('echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"')
            }

        }
        buildEnv(job)
        buildCommon(job)
    }
    void sendReviewNotificationMail(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "send-review-notification-mail"){
            String mailContent = '''<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
                    <title>Message Title</title>
                </head>
                <body class="jira" style="color: #333; font-family: Arial, sans-serif; font-size: 14px; line-height: 1.429">
                    <table id="background-table" cellpadding="0" cellspacing="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #f5f5f5; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt">
                        <!-- header here -->
                        <tr>
                            <td id="header-pattern-container" style="padding: 0px; border-collapse: collapse; padding: 10px 20px">
                             </td>
                        </tr>
                        <tr>
                            <td id="email-content-container" style="padding: 0px; border-collapse: collapse; padding: 0 20px">
                                <table id="email-content-table" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0; border-collapse: separate">
                                    <tr>
                                        <!-- there needs to be content in the cell for it to render in some clients -->
                                        <td class="email-content-rounded-top mobile-expand" style="padding: 0px; border-collapse: collapse; color: #fff; padding: 0 15px 0 16px; height: 15px; background-color: #fff; border-left: 1px solid #ccc; border-top: 1px solid #ccc; border-right: 1px solid #ccc; border-bottom: 0; border-top-right-radius: 5px; border-top-left-radius: 5px; height: 10px; line-height: 10px; padding: 0 15px 0 16px; mso-line-height-rule: exactly">
                                            &nbsp;
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="email-content-main mobile-expand " style="padding: 0px; border-collapse: collapse; border-left: 1px solid #ccc; border-right: 1px solid #ccc; border-top: 0; border-bottom: 0; padding: 0 15px 0 16px; background-color: #fff">
                                            <table class="page-title-pattern" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt">
                                                <tr>
                                                    <td style="vertical-align: top;; padding: 0px; border-collapse: collapse; padding-right: 5px; font-size: 20px; line-height: 30px; mso-line-height-rule: exactly" class="page-title-pattern-header-container"> <span class="page-title-pattern-header" style="font-family: Arial, sans-serif; padding: 0; font-size: 20px; line-height: 30px; mso-text-raise: 2px; mso-line-height-rule: exactly; vertical-align: middle"> <a href="" style="color: #3b73af; text-decoration: none">${product_name}${product_version}${product_milestone}  Release Handover Review</a> </span>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td id="text-paragraph-pattern-top" class="email-content-main mobile-expand  comment-top-pattern" style="padding: 0px; border-collapse: collapse; border-left: 1px solid #ccc; border-right: 1px solid #ccc; border-top: 0; border-bottom: 0; padding: 0 15px 0 16px; background-color: #fff; border-bottom: none; padding-bottom: 0">
                                            <table class="text-paragraph-pattern" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; font-family: Arial, sans-serif; font-size: 14px; line-height: 20px; mso-line-height-rule: exactly; mso-text-raise: 2px">
                                                <tr>
                                                    <td class="text-paragraph-pattern-container mobile-resize-text " style="padding: 0px; border-collapse: collapse; padding: 0 0 10px 0">
                                                        <p style="margin: 10px 0 0 0">Release handover is <font color="#ff0000">waiting for review</font> in remote: <a href="${handover_pr}" class="external-link" rel="nofollow" style="color: #3b73af; text-decoration: none">${handover_pr}</a></p>
                                                        <p style="margin: 10px 0 0 0">Staging folder URL: <br /> <a href="${rcm_staging_base}/${product_staging_path}/" class="external-link" rel="nofollow" style="color: #3b73af; text-decoration: none">${rcm_staging_base}/${product_staging_path}/</a></p>
                                                        <p style="margin: 10px 0 0 0"><a href="${rcm_staging_base}/${product2_staging_path}/" class="external-link" rel="nofollow" style="color: #3b73af; text-decoration: none">${rcm_staging_base}/${product2_staging_path}/</a></p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="email-content-main mobile-expand " style="padding: 0px; border-collapse: collapse; border-left: 1px solid #ccc; border-right: 1px solid #ccc; border-top: 0; border-bottom: 0; padding: 0 15px 0 16px; background-color: #fff">
                                            <table id="actions-pattern" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; font-family: Arial, sans-serif; font-size: 14px; line-height: 20px; mso-line-height-rule: exactly; mso-text-raise: 1px">
                                                <tr>
                                                    <td id="actions-pattern-container" valign="middle" style="padding: 0px; border-collapse: collapse; padding: 10px 0 10px 24px; vertical-align: middle; padding-left: 0">
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <!-- there needs to be content in the cell for it to render in some clients -->
                                    <tr>
                                        <td class="email-content-rounded-bottom mobile-expand" style="padding: 0px; border-collapse: collapse; color: #fff; padding: 0 15px 0 16px; height: 5px; line-height: 5px; background-color: #fff; border-top: 0; border-left: 1px solid #ccc; border-bottom: 1px solid #ccc; border-right: 1px solid #ccc; border-bottom-right-radius: 5px; border-bottom-left-radius: 5px; mso-line-height-rule: exactly">
                                            &nbsp;
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <td id="footer-pattern" style="padding: 0px; border-collapse: collapse; padding: 12px 20px">
                                <table id="footer-pattern-container" cellspacing="0" cellpadding="0" border="0" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt">
                                    <tr>
                                        <td id="footer-pattern-text" class="mobile-resize-text" width="100%" style="padding: 0px; border-collapse: collapse; color: #999; font-size: 12px; line-height: 18px; font-family: Arial, sans-serif; mso-line-height-rule: exactly; mso-text-raise: 2px">
                                            This message was sent by <span id="footer-build-information">${BUILD_URL} </span>
                                        </td>
                                        <td id="footer-pattern-logo-desktop-container" valign="top" style="padding: 0px; border-collapse: collapse; padding-left: 20px; vertical-align: top">
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
            </html>
            '''

            def shellScript = '''
            set +x
            echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            jira_comment="Release handover is {color:#ff0000}waiting for review{color}  in \${handover_pr} \n
            Staging folder URL: \n
            [\${rcm_staging_base}/\${product_staging_path}/]\n
            [\${rcm_staging_base}/\${product2_staging_path}/]"
            kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
            ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -f -a "\${jira_comment}"
            '''
            // Sets a description for the job.
            description("This job is responsible for sending the PR review email to the team.")
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }
            if(jobName.matches("(.*)codereview/(.*)")){
                println "Detected in codereview:Disable send-review-notification-mail"
                disabled()
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
                            'pattern' "stream/${release_code}/release-history/*"
                        }
                    }
                }
            }
            // Adds post-build actions to the job.
            publishers {

                // Sends customizable email notifications.
                extendedEmail {

                    // Adds email addresses that should receive emails.
                    recipientList('${release_engineer}')

                    // Adds e-mail addresses to use in the Reply-To header of the email.
                    replyToList('${release_engineer}')

                    // Sets the default email subject that will be used for each email that is sent.
                    defaultSubject('[ACTION REQUIRED] [BxMS Release CI] ${product_name}${release_milestone_version}${release_milestone}  Release Handover Review')

                    // Sets the default email content that will be used for each email that is sent.
                    defaultContent(mailContent)

                    // Sets the content type of the emails sent after a build.
                    contentType('text/html')
                    triggers {
                        always{
                            sendTo {
                                recipientList()
                            }
                        }
                    }
                }
            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void stageBrewBuild(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "stage-brew-build"){
            def shellScript = '''
            set -x
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            #Dynamic variable of bash
            product_assembly_maven_repo_url=${product_lowercase}_assembly_maven_repo_url
            product_installer_maven_repo_url=${product_lowercase}_installer_maven_repo_url

            ip-tooling/maven-artifact-handler.py --version=${product_artifact_version} --override-version ${product_upload_version} \\
                --maven-repo ${!product_assembly_maven_repo_url} --deliverable ${product_deliverable_template} --output ${product_lowercase}
            #Download installer
            ip-tooling/maven-artifact-handler.py --version=${product_artifact_version} --override-version ${product_upload_version} \\
                --maven-repo ${!product_installer_maven_repo_url} --deliverable ${product_deliverable_template} --output ${product_lowercase}
            #Download runtime gav txt
            ip-tooling/maven-artifact-handler.py --version=${product_artifact_version} --override-version ${product_upload_version} \\
                --maven-repo ${license_builder_maven_repo_url} --deliverable ${product_deliverable_template} --output ${product_lowercase}

            #rename license-builder \${product_lowercase}-runtime-GAV \${product_lowercase}/*.txt
            cp ${IP_CONFIG_FILE} ${product_lowercase}/
            '''
            // Sets a description for the job.
            description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

            // Allows to parameterize the job.
            parameters {

                // Defines a simple text parameter, where users can enter a string value.
                booleanParam("CLEAN_STAGING_ARTIFACTS", false, "WARNING, click this will force remove your artifacts in staging folder!")
            }

            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)

            }
            wrappers {
                // Deletes files from the workspace before the build starts.
                preBuildCleanup()

            }
            // Adds post-build actions to the job.
            publishers {

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                            // Adds a target server.
                            verbose(true)

                            // Adds a transfer set.
                            transferSet {

                                // Sets the files to upload to a server.
                                sourceFiles('')
                                // Sets the destination folder.
                                remoteDirectory('${product_staging_path}')
                                execCommand('if [ "${CLEAN_STAGING_ARTIFACTS}" = "true" ];then \n' +
                                                'rm -vrf  ~/staging/${product_staging_path}/* \n' +
                                            'fi')
                            }
                            // Adds a target server.
                            verbose(true)

                            // Adds a transfer set.
                            transferSet {

                                // Sets the files to upload to a server.
                                sourceFiles('${product_lowercase}/*.*')
                                removePrefix('${product_lowercase}/')

                                // Sets the destination folder.
                                remoteDirectory('${product_staging_path}')
                            }
                    }
                }
            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    //This needs testing
    void stageBxmsPatch(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "stage-bxms-patch"){
            def shellScript = '''
            #Uploading to rcm staging folder
            echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
            ip-tooling/maven-artifact-handler.py --version=\${product_artifact_version} --override-version \${product_version} \
               --deliverable \${product_deliverable_template} --maven-repo \${bxms_patch_maven_repo_url} \
               --output \${product_name}

            '''
            // Sets a description for the job.
            description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

            disabled()
            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }

            wrappers {
                // Deletes files from the workspace before the build starts.
                preBuildCleanup()

            }
            // Adds post-build actions to the job.
            publishers {

                // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
                publishOverSsh {

                    // Adds a target server.
                    server('publish server') {

                        // Adds a target server.
                        verbose(true)

                        // Adds a transfer set.
                        transferSet {

                            // Sets the files to upload to a server.
                            sourceFiles('${product_name}/*.*')
                            removePrefix('${product_name}/')

                            // Sets the destination folder.
                            remoteDirectory('${product_staging_path}')
                        }

                        // Adds a transfer set.
                        transferSet {

                            // Sets the files to upload to a server.
                            sourceFiles('${product2_name}/*.*')
                            removePrefix('${product2_name}/')

                            // Sets the destination folder.
                            remoteDirectory('${product2_staging_path}')
                        }

                        // Adds a transfer set.
                        transferSet {

                            // Sets the files to upload to a server.
                            sourceFiles('${release_code}-deliverable-list*.properties')

                            // Sets the destination folder.
                            remoteDirectory('${product_staging_path}/')
                        }

                        // Adds a transfer set.
                        transferSet {

                            // Sets the files to upload to a server.
                            sourceFiles('${release_code}-deliverable-list*.properties')

                            // Sets the destination folder.
                            remoteDirectory('${product2_staging_path}/')
                        }
                    }
                }
            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void startBrewBuild(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "start-brew-build"){
            def shellScript = '''
            echo -e "Exec node IP:${OPENSTACK_PUBLIC_IP}\\n"
            kinit -k -t ${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

            UNBLOCK=1 BREWCHAIN=1 CFG=./${IP_CONFIG_FILE} POMMANIPEXT=${product_lowercase}-build-bom make -f  ${makefile} ${product_lowercase}-installer 2>&1| tee b.log

            brewchain_build_url=`grep 'build: Watching task ID:' b.log`
            brewchain_build_url=`python -c "import sys,re;print re.match('^.*(https.*\\d+).*$', '$brewchain_build_url').group(1)"`

            echo "Brewchain Build URL: $brewchain_build_url"

            sed -i '/^brewchain_build_url=/d' ${CI_PROPERTIES_FILE} && echo "brewchain_build_url=$brewchain_build_url" >>${CI_PROPERTIES_FILE}
            ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Brew chainbuild is trigger at: ${brewchain_build_url}" -f
            '''
            // Sets a description for the job.
            description("This job is responsible for initialising the Brew chain build.")
            if(jobName.matches("(.*)codereview/(.*)")&& !release_code.matches("(.*)-test(.*)")){
                println "Detected in codereview:Disable trigger-qe-smoketest"
                disabled()
            }
            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void triggerQeSmokeTest(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "trigger-qe-smoke-test"){
            def shellScript = '''
            kinit -k -t  ${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM


            ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "QE smoketest is triggered by CI message. Build URL: ${qe_smoketest_job_url}" -f
            '''
            // Sets a description for the job.
            description("This job is responsible for triggering QE smoke test.")

            if(jobName.matches("(.*)codereview/(.*)")){
                println "Detected in codereview:Disable trigger-qe-smoketest"
                disabled()
            }

            // Adds build steps to the jobs.
            steps {
                shell(shellScript)

                // Sends JMS message.
                ciMessageBuilder {
                    overrides {
                        topic('VirtualTopic.qe.ci.ba.${product_lowercase}.${product_version_major}${product_version_minor}.${release_type}.trigger')
                    }

                    // JMS selector to choose messages that will fire the trigger.
                    providerName("Red Hat UMB")

                    // Type of CI message to be sent.
                    messageType("Custom")

                    // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
                    messageProperties('label=rhba-ci\n' +
                            'CI_TYPE=custom\n' +
                            'EVENT_TYPE=${product_lowercase}-${product_version_major}${product_version_minor}-${release_type}-qe-trigger\n')
                    // Content of CI message to be sent.
                    messageContent('${product_staging_properties_url}')
                }
            }
        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void updateProductJira(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "update-product-jira"){
            def shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}\\n"
            echo "The product version is $product_version and the release milestone is $product_milestone."
            kinit -k -t  ${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
            python ip-tooling/release-ticketor.py --user mw-prod-ci --password ds54sdfs54df \
                --headless $product_version.GA $cutoff_date $product_version.$product_milestone 2>&1 | tee /tmp/release-ticketor-output

            sed -i '/^resolve_issue_list=/d' ${CI_PROPERTIES_FILE} \
                && echo "resolve_issue_list="`cat /tmp/release-ticketor-output | grep https://url.corp.redhat.com` >> ${CI_PROPERTIES_FILE}
            '''
            // Sets a description for the job.
            description("This job is responsible for updating the community JIRA tickets associated with this release.")

            // Adds build steps to the jobs.
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }

        }
        buildEnv(job)
        buildCommon(job)
        buildScm(job)
    }
    void validateQeProperties(DslFactory dslFactory){
        def job=dslFactory.job(release_code + "-release-pipeline/" + release_code +"-"+ "validate-qe-properties"){
            String shellScript = '''
            echo -e "Exec node IP: ${OPENSTACK_PUBLIC_IP}"
            if [ "$release_status" = "closed" ];then
                    exit 0
            fi

            wget ${product_staging_properties_url} -O ${product_staging_properties_name}

            echo -e "import sys,os,re;
from urllib2 import urlopen;
def isvalidurl(url, inc_str):
    ret=0
    print url
    try:
        code = urlopen(url).code
    except IOError:
        print 'ERROR ', url + ' is invalid!'
        ret=1
        return ret
    if (code / 100 >= 4):
        print 'ERROR ', url + ' is invalid!'
        ret=1
    ret+=assertContain(url, inc_str)
    return ret

def assertEqual(expect, actual):
    ret=0
    if expect != actual:
        print 'ERROR Actual is ' + actual + ' , Expect is ' + expect
        ret=1
    return ret

def assertContain(actual, expect):
    ret=0
    if expect not in actual:
        print 'ERROR Actual is ' + actual + ' , Expect is ' + expect
        ret=1
    return ret

def validateProperties(propfile, keyword, _product_name):
    ret=0
    dic = {}
    if os.path.isfile(propfile):
        tmpFile = open(propfile, 'r')
        for line in tmpFile:
            str1, tmp, str2 = line.partition('=')
            str2 = str2.replace('\\\\\\\\n', '')
            dic[str1] = str2
        tmpFile.close()
        if re.match('rhdm-.*', propfile) is not None:
            ret+=isvalidurl(dic['rhdm.addons.latest.url'],keyword)
            ret+=isvalidurl(dic['rhdm.kie-server.ee7.latest.url'],keyword)
            ret+=isvalidurl(dic['rhdm.decision-central.standalone.latest.url'],keyword)
            ret+=isvalidurl(dic['rhdm.decision-central-eap7.latest.url'],keyword)
            ret+=isvalidurl(dic['rhdm.installer.latest.url'],keyword)

            if '${release_type}' != 'nightly':
                ret+=isvalidurl(dic['rhdm.maven.repo.latest.url'],keyword)
                ret+=isvalidurl(dic['rhdm.sources.latest.url'],keyword)

            ret+=assertEqual('$kie_version', dic['KIE_VERSION'])
            ret+=assertEqual('${product_artifact_version}', dic['RHDM_VERSION'])
            ret+=assertContain(dic['rhdm.decision-central.standalone.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['rhdm.addons.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['rhdm.kie-server.ee7.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['rhdm.installer.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['INSTALLER_COMMONS_VERSION'], '$installercommons_version')
            ret+=assertContain(dic['IZPACK_VERSION'], '$izpack_version')
            ret+=assertContain(dic['JAVAPARSER_VERSION'], '${drlx_parser_version}')

        if re.match('rhpam..*', propfile) is not None:
            ret+=isvalidurl(dic['rhpam.addons.latest.url'],keyword)
            ret+=isvalidurl(dic['rhpam.kie-server.ee7.latest.url'],keyword)
            ret+=isvalidurl(dic['rhpam.business-central.standalone.latest.url'],keyword)
            ret+=isvalidurl(dic['rhpam.business-central-eap7.latest.url'],keyword)
            ret+=isvalidurl(dic['rhpam.installer.latest.url'],keyword)

            if '${release_type}' != 'nightly':
                ret+=isvalidurl(dic['rhpam.maven.repo.latest.url'],keyword)
                ret+=isvalidurl(dic['rhpam.sources.latest.url'],keyword)
                ret+=isvalidurl(dic['build.config'], _product_name)

            ret+=assertEqual(dic['KIE_VERSION'], '$kie_version')
            ret+=assertEqual(dic['RHPAM_VERSION'], '${product_artifact_version}')
            ret+=assertContain(dic['rhpam.business-central.standalone.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['rhpam.addons.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['rhpam.kie-server.ee7.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['rhpam.installer.latest.url'], '$product_milestone_version')
            ret+=assertContain(dic['INSTALLER_COMMONS_VERSION'], '$installercommons_version')
            ret+=assertContain(dic['IZPACK_VERSION'], '$izpack_version')
            ret+=assertContain(dic['JAVAPARSER_VERSION'], '${drlx_parser_version}')

        if ret != 0:
            print propfile + ' Validation No Pass'
            assert ret == 0
        else:
            print propfile + ' Validation Pass'
    else:
        print 'error!'
        sys.exit(1)
print '---Exec the py script...---'
validateProperties(sys.argv[1], sys.argv[2],sys.argv[3])
">validateProperties.py
            if [ "\${IS_STAGING}" == "true" ];then
                if [ "\${release_type}" == "nightly" ];then
                    python validateProperties.py "${product_staging_properties_name}" "rhap-repo" "${product_lowercase}"
                else
                    python validateProperties.py "${product_staging_properties_name}" "rcm-guest" "${product_lowercase}"
                fi
            else
                python validateProperties.py "${product_staging_properties_name}" "candidates" "${product_lowercase}"
            fi
            '''
            // Sets a description for the job.
            description("This job is responsible for uploading release to candidate area.")
            steps {

                // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                shell(shellScript)
            }

            // Allows to parameterize the job.
            parameters {
                // Defines a simple text parameter, where users can enter a string value.
                booleanParam("IS_STAGING",  true,"Specify product name to switch between configurations.")
            }
        }
        buildEnv(job)
        buildCommon(job)
    }
    void buildCommon(Job job, String node_label="release-pipeline"){
        job.with{
            label(node_label)

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

            environmentVariables {
                // The name of the product, e.g., bxms64.
                env("RELEASE_CODE", release_code)

                // Release pipeline CI properties file
                env("CI_PROPERTIES_FILE",ci_properties_file)

                // IP project configuration file
                env("IP_CONFIG_FILE", cfg_file)

                env("GERRIT_REFSPEC", gerritRefspec)

                env("GERRIT_BRANCH", gerritBranch)
                // Adds environment variables from a properties file.
                propertiesFile(ci_properties_file)

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

            }

        }

    }
    void buildScm(Job job){
        job.with{
            // Allows a job to check out sources from multiple SCM providers.
            multiscm {

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                        refspec("+refs/heads/master:refs/remotes/origin/master")
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
                        refspec("+refs/heads/master:refs/remotes/origin/master")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("FETCH_HEAD")

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
                        name("origin")
                        refspec(gerritRefspec)
                    }

                    // Specify the branches to examine for changes and to build.
                    branch(gerritBranch)

                    // Adds additional behaviors.
                    extensions {
                        // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                        relativeTargetDirectory('bxms-jenkins')
                    }
                }
            }
        }
    }
    void buildEnv(Job job){
        job.with{
            environmentVariables {

                // The name of the product, e.g., bxms64.
                env("RELEASE_CODE", release_code)

                // Release pipeline CI properties file
                env("CI_PROPERTIES_FILE",ci_properties_file)

                // IP project configuration file
                env("IP_CONFIG_FILE", cfg_file)

                env("GERRIT_REFSPEC", gerritRefspec)

                env("GERRIT_BRANCH", gerritBranch)

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

            }
        }
    }
    Job build(DslFactory dslFactory) {
        dslFactory.folder(release_code + "-release-pipeline")

        brewRepoRegen(dslFactory)
        monitoringCimessage(dslFactory)
        createHandover(dslFactory)
        createProductTag(dslFactory)
        releaseNotes(dslFactory)
        exportOpenshiftImages(dslFactory)
        generateQeProperties(dslFactory)
        generateSources(dslFactory)
        initRelease(dslFactory)
        locateImportList(dslFactory)
        mavenRepositoryBuild(dslFactory)
        mockQeSmoketestReport(dslFactory)
        openshiftTest(dslFactory)
        promoteRelease(dslFactory)
        pvtTest(dslFactory)
        sendHandoverMail(dslFactory)
        sendReviewNotificationMail(dslFactory)
        stageBrewBuild(dslFactory)
        stageBxmsPatch(dslFactory)
        startBrewBuild(dslFactory)
        triggerQeSmokeTest(dslFactory)
        updateProductJira(dslFactory)
        validateQeProperties(dslFactory)

    }
}
