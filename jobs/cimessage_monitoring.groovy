import org.jboss.bxms.jenkins.JobTemplate

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
    sed -i '/^\$1/d' ${CI_PROPERTIES_FILE} && echo "\$1=\$2" >> ${CI_PROPERTIES_FILE}
}
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
if [ "\$CI_TYPE" = "brew-tag" ];then
        version=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['version']"` 1>/dev/null
        release=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['release']"` 1>/dev/null
        task_id=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['task_id']"` 1>/dev/null
        nvr=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['nvr']"` 1>/dev/null
    if [ "\$CI_NAME" = "org.jboss.ip-brms-bpmsuite-assembly" ];then
        product_assembly_maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
        product_assembly_brew_url="https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
        product_nvr="\$nvr"
        appendProp "product_assembly_maven_repo_url" \$product_assembly_maven_repo_url
        appendProp "product_assembly_brew_url" \$product_assembly_brew_url
        appendProp "product_nvr" \$product_nvr
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Product Assembly Build Completed: \$product_assembly_brew_url Build nvr: \$product_nvr " -f
        
    elif [ "\$CI_NAME" = "org.jboss.brms-bpmsuite.patching-patching-tools-parent" ];then
        bxms_patch_maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"
        bxms_patch_brew_url="https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=\${task_id}"
        bxms_patch_nvr="\$nvr"
        appendProp "bxms_patch_maven_repo_url" \$bxms_patch_maven_repo_url
        appendProp "bxms_patch_maven_repo_url" \$bxms_patch_maven_repo_url
        appendProp "bxms_patch_nvr" \$bxms_patch_nvr
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Patch Brew Build Completed: \$bxms_patch_brew_url nvr:\$bxms_patch_nvr " -f
    elif [ "\$CI_NAME" = "org.jboss.ip-bxms-maven-repo-root" ];then
        #Trigger maven repo to build
        echo "maven-repo-root build has been completed"
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "All brew build completed: \$brewchain_build_url. Ready to trigger maven repo build" -f
    fi
elif [ "\$label" = "bxms-ci" ];then
    if [[ "\$CI_NAME" =~ smoketest-trigger\$ ]];then
        echo "Triggered by  bxms-prod ci message "
        echo "\$CI_MESSAGE" 
    elif [[ "\$CI_NAME" =~ smoketest-report\$ ]];then
        echo "QE smoketest report:\$CI_MESSAGE"
        #Json to adoc
        echo \${CI_MESSAGE}| python -c "import sys, json; 
_report=json.load(sys.stdin)
adoc_file=open('\${qe_smoketest_report_path}', 'w')
adoc_file.write('=== QE smoketest Report\\n')
adoc_file.write('[width=100%,options=header,footer,align=center,frame=all]\\n')
adoc_file.write('|============\\n')
adoc_file.write('|SUMMARY|\\n')
_failure=[]
_unstable=[]
for key in _report:
    if ('SUCCESS' in _report[key]):
        continue
    elif ('FAILURE' in _report[key]):
        _failure.append(key)
        continue
    elif ('UNSTABLE' in _report[key]):
        _unstable.append(key)
        continue
    adoc_file.write('|' + key + '|' + _report[key] +'\\n')
    

adoc_file.write('|#FAILURE#|')
adoc_file.write('\\n\\n'.join(map(str,_failure)))
adoc_file.write('\\n')
adoc_file.write('|#UNSTABLE#|')
adoc_file.write('\\n\\n'.join(map(str,_unstable)))
adoc_file.write('\\n')
adoc_file.write('|URL|\${qe_smoketest_job_url}')
adoc_file.write('|============\\n')
adoc_file.close()
"       cat \${qe_smoketest_report_path}.adoc
        ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "QE smoketest returned" -f
    else
        echo "Something else triggered this job"
        echo "\$CI_MESSAGE"
    fi
elif [ "\$new" = "FAILED" ];then
    ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Brewchain failed: \$brewchain_build_url " -f
else
    echo "ERROR!Not triggered by CI!"
    exit 1
fi
"""
// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-monitoring-cimessage") {

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    triggers{
        ciBuildTrigger {
            selector("label='bxms-ci' OR (CI_TYPE='brew-tag' AND ( CI_NAME='org.jboss.ip-bxms-maven-repo-root' OR label='bxms-ci' OR CI_NAME='org.jboss.ip-brms-bpmsuite-assembly' OR CI_NAME='org.jboss.brms-bpmsuite.patching-patching-tools-parent')) OR (new='FAILED' AND method='chainmaven' AND (target='jb-bxms-7.0-maven-candidate' OR target='jb-bxms-6.4-candidate'))")
            providerName('default')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)