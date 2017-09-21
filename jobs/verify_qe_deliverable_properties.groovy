import org.jboss.bxms.jenkins.JobTemplate
String shellScript = """
wget \${brms_staging_properties_url} -O \${brms_staging_properties_name} 
wget \${brms_candidate_properties_url} -O \${brms_candidate_properties_name}

python -c "import sys,os
from urllib2 import urlopen
ret=0
def isvalidurl(url, inc_str):
    print url
    try:
        code = urlopen(url).code
    except IOError:
        print 'ERROR ', url + 'is invalid!'
        ret=1
        return 1
    if (code / 100 >= 4):
        print 'ERROR ', url + 'is invalid!'
        ret=1
    assertContain(url, inc_str)

def assertEqual(expect, actual):
    if expect != actual:
        print 'ERROR Actual is ' + actual + ' , Expect is ' + expect
        ret=1

def assertContain(actual, expect):
    if expect not in actual:
        print 'ERROR Actual is ' + actual + ' , Expect is ' + expect
        ret=1

def validateProperties(propfile, keyword):
    ret = 0
    dic = {}
    if os.path.isfile(propfile):
        tmpFile = open(propfile, 'r')
        for line in tmpFile:
            str1, tmp, str2 = line.partition('=')
            str2 = str2.replace('\\n', '')
            dic[str1] = str2
        tmpFile.close()
        isvalidurl(dic['brms.collection.latest.url'],keyword)
        isvalidurl(dic['bpms.collection.latest.url'],keyword)
        isvalidurl(dic['brms.execution-server.ee7.latest.url'],keyword)
        isvalidurl(dic['bpms.execution-server.ee7.latest.url'],keyword)
        isvalidurl(dic['bxms.maven.repo.latest.url'],keyword)        
        isvalidurl(dic['build.config'],keyword)

        assertEqual('\$kie_version', dic['bxms.maven.repo.latest.url'])
        assertEqual('\$product_artifact_version', dic['BXMS_VERSION'])
#        assertContain(dic['brms.business-central.standalone.latest.url'], '\$product_deliver_version')
        assertContain(dic['brms.collection.latest.url'], '\$product_deliver_version')
        assertContain(dic['brms.execution-server.ee7.latest.url'], '\$product_deliver_version')
        assertContain(dic['bpms.collection.latest.url'], '\$product_deliver_version')
        assertContain(dic['bpms.execution-server.ee7.latest.url'], '\$product_deliver_version')
#        assertContain(dic['bxms.execution-server.jws.latest.url'], '\$product_deliver_version')
        if ret != 0:
            print propfile + ' Validation No Pass'
            sys.exit(1)
        else:
            print  propfile + ' Validation Pass'   
    else:
        return 1

validateProperties('\$brms_staging_properties_name', 'rcm-guest')    
validateProperties('\${brms_candidate_properties_name}', 'candidates')
"
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
                    'pattern' '.*-release/.*-handover.adoc'
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)