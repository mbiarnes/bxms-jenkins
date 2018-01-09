import org.jboss.bxms.jenkins.JobTemplate
import org.apache.commons.lang.RandomStringUtils

def shellScript = """# Disable bash tracking mode, too much noise.
set -x

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
    sed -i "/^\$1/d" ${CI_PROPERTIES_FILE} && echo "\$1=\$2" >> ${CI_PROPERTIES_FILE}
}
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
if [ "\$CI_TYPE" = "brew-tag" ];then
        if [ "\$brew_status" != "running" ];then
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
    if [ "\$CI_NAME" = "org.kie.rhap-rhbas" ];then
        product2_assembly_maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
        product2_assembly_brew_url="https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
        product2_nvr="\$nvr"
        appendProp "product2_assembly_maven_repo_url" \$product2_assembly_maven_repo_url
        appendProp "product2_assembly_brew_url" \$product2_assembly_brew_url
        appendProp "product2_nvr" \$product2_nvr
        web_hook=`grep "register_web_hook" ${CI_PROPERTIES_FILE} |cut -d "=" -f2`
        curl -X POST -d 'OK' -k \$web_hook
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Product Assembly Build Completed: \$product2_assembly_brew_url Build nvr: \$product2_nvr " -f

    elif [ "\$CI_NAME" = "org.kie.rhap-rhdm" ];then
        product1_assembly_maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
        product1_assembly_brew_url="https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
        product1_nvr="\$nvr"
        appendProp "product1_assembly_maven_repo_url" \$product1_assembly_maven_repo_url
        appendProp "product1_assembly_brew_url" \$product1_assembly_brew_url
        appendProp "product1_nvr" \$product1_nvr
        #web_hook=`grep "register_web_hook" ${CI_PROPERTIES_FILE} |cut -d "=" -f2`
        #curl -X POST -d 'OK' -k \$web_hook
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Product Assembly Build Completed: \$product1_assembly_brew_url Build nvr: \$product1_nvr " -f
    elif [ "\$CI_NAME" = "org.jboss.brms-bpmsuite.patching-patching-tools-parent" ];then
        bxms_patch_maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
        bxms_patch_brew_url="https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
        bxms_patch_nvr="\$nvr"
        appendProp "bxms_patch_maven_repo_url" \$bxms_patch_maven_repo_url
        appendProp "bxms_patch_maven_repo_url" \$bxms_patch_maven_repo_url
        appendProp "bxms_patch_nvr" \$bxms_patch_nvr
        web_hook=`grep "register_web_hook" ${CI_PROPERTIES_FILE} |cut -d "=" -f2`
        curl -X POST -d 'OK' -k \$web_hook
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Patch Brew Build Completed: \$bxms_patch_brew_url nvr:\$bxms_patch_nvr " -f
    elif [ "\$CI_NAME" = "org.jboss.ip-bxms-maven-repo-root" ];then
        #Trigger maven repo to build
        echo "maven-repo-root build has been completed"
        #web_hook=`grep "register_web_hook" ${CI_PROPERTIES_FILE} |cut -d "=" -f2`
        #curl -X POST -d 'OK' -k \$web_hook
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Maven repo root build completed. Ready to trigger maven repo build" -f
    fi
elif [ "\$label" = "rhap-ci" ];then
    if [ "\$release_status" = "closed" ];then
        exit 0
    fi
    if [[ "\$EVENT_TYPE" =~ 70-brew-qe-trigger\$ ]];then
        echo "Triggered by  bxms-prod ci message "
        echo "\$CI_MESSAGE"
    elif [[ "\$EVENT_TYPE" =~ qe-smoke-results\$ ]];then
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
    web_hook=`grep "register_web_hook" ${CI_PROPERTIES_FILE} |cut -d "=" -f2`
    curl -X POST -d 'OK' -k \$web_hook
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "QE smoketest returned" -f
    else
        echo "Something else triggered this job"
        echo "\$CI_MESSAGE"
    fi
elif [ "\$new" = "FAILED" ] && [ "\$method" = "chainmaven" ];then
    ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Brewchain failed: \$brewchain_build_url " -f
    web_hook=`grep "register_web_hook" ${CI_PROPERTIES_FILE} |cut -d "=" -f2`
    curl -X POST -d 'STOP' -k \$web_hook
else
    echo "ERROR!Not triggered by CI!"
    exit 1
fi

"""
int randomStringLength = 32
String charset = (('a'..'z') + ('A'..'Z') + ('0'..'9')).join()
String randomString = RandomStringUtils.random(randomStringLength, charset.toCharArray())
print randomString
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-monitoring-cimessage") {

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    triggers{
        ciBuildTrigger {
            overrides {
              topic("Consumer.rh-jenkins-ci-plugin.${randomString}.VirtualTopic.qe.ci.>")
            }
            selector("label='rhap-ci' OR (CI_TYPE='brew-tag' AND ( CI_NAME='org.jboss.ip-bxms-maven-repo-root' OR CI_NAME='org.kie.rhap-rhdm' OR CI_NAME='org.kie.rhap-rhbas' OR CI_NAME='org.jboss.brms-bpmsuite.patching-patching-tools-parent')) OR (new='FAILED' AND method='chainmaven' AND target='jb-bxms-7.0-maven-candidate')")
            //providerName('CI Publish')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
