import org.jboss.bxms.jenkins.JobTemplate
String shellScript = """
wget \${brms_staging_properties_url} -O \${brms_staging_properties_name} 
wget \${brms_candidate_properties_url} -O \${brms_candidate_properties_name}

function assertContain(){
    if [ ! -z "\$1" ] || [ ! -z "\$2" ];then
        echo "Param is empty"
        exit 1
    fi
    if [ "\$1" =~ \$2];then
        echo ""
    else
        echo "Validation failed, \$1 don't contain string \$2"
        exit 1;
    fi
}
function isvalidurl(){
    if [ ! -z "\$1" ];then
        echo "Param is empty"
        exit 1
    fi
    if curl --output /dev/null --silent --head --fail "\$1";then
        echo "URL exists: \$1"
    else
        echo "URL does not exist: \$1"
        exit 1
    fi
    #If provie second param, verify it contians the string
    if [ -z "\$2" ];then
        assertContain("\$1", "\$2")
    fi
}
function assertEqual(){
    if [ ! -z "\$1" ] || [ ! -z "\$2" ];then
        echo "Param is empty"
        exit 1
    fi
    if [ "\$1" != "\$2" ];then
        echo "Expected \$1, But: \$2
        exit 1
    fi
}

#Verify the staging properties
source \${brms_staging_properties_name} 
isvalidurl \$brms.business-central.standalone.latest.url "rcm-guest"
isvalidurl \$brms.collection.latest.url "rcm-guest"
isvalidurl \$bpms.business-central.standalone.latest.url "rcm-guest"
isvalidurl \$bpms.collection.latest.url "rcm-guest"
isvalidurl \$bxms.execution-server.ee7.latest.url "rcm-guest"
isvalidurl \$bxms.execution-server.jws.latest.url "rcm-guest"
isvalidurl \$build.config "rcm-guest"
isvalidurl \$bxms.maven.repo.latest.url "rcm-guest"
assertEqual "\$kie_version" "\$bxms.maven.repo.latest.url"
assertEqual "\$product_artifact_version" "\$BXMS_VERSION"
assertContain "\$brms.business-central.standalone.latest.url" "\$product_deliver_version"
assertContain "\$brms.collection.latest.url" "\$product_deliver_version"
assertContain \$bxms.execution-server.ee7.latest.url "\$product_deliver_version"
assertContain \$bxms.execution-server.jws.latest.url "\$product_deliver_version"

#Verify the staging properties
source \${brms_candidate_properties_name}
isvalidurl \$brms.business-central.standalone.latest.url "candidates"
isvalidurl \$brms.collection.latest.url "candidates"
isvalidurl \$bpms.business-central.standalone.latest.url "candidates"
isvalidurl \$bpms.collection.latest.url "candidates"
isvalidurl \$bxms.execution-server.ee7.latest.url "candidates"
isvalidurl \$bxms.execution-server.jws.latest.url "candidates"
isvalidurl \$build.config "candidates"
isvalidurl \$bxms.maven.repo.latest.url
assertEqual "\$kie_version" "\$bxms.maven.repo.latest.url"
assertEqual "\$product_artifact_version" "\$BXMS_VERSION"
assertContain "\$brms.business-central.standalone.latest.url" "\$product_deliver_version"
assertContain "\$brms.collection.latest.url" "\$product_deliver_version"
assertContain \$bxms.execution-server.ee7.latest.url "\$product_deliver_version"
assertContain \$bxms.execution-server.jws.latest.url "\$product_deliver_version"
"""
// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-verify-deliverable-properties") {

    // Sets a description for the job.
    description("This job is responsible for uploading release to candidate area.")
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
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
                    'pattern' '*-release/*-handover.adoc'
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)