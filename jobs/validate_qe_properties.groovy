import org.jboss.bxms.jenkins.JobTemplate

String shellScript = """
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}"
if [ "\$release_status" = "closed" ];then
        exit 0
fi
case "\${PRODUCT_NAME}" in
        RHDM )
            product_staging_properties_url=\${product1_staging_properties_url}
            product_staging_properties_name=\${product1_staging_properties_name}
            product_version=\${product1_version}
        ;;
        RHBAS )
            product_staging_properties_url=\${product2_staging_properties_url}
            product_staging_properties_name=\${product2_staging_properties_name}
            product_version=\${product2_version}
        ;;
esac

if [ "\${release_code}" == "bxms-nightly" ]; then
        product_staging_properties_name="\${PRODUCT_NAME}-\${build_date}.properties"
        product_staging_properties_url="\${rcm_staging_base}/\${PRODUCT_NAME,,}/\${PRODUCT_NAME}-\${product_version}.NIGHTLY/\${product_staging_properties_name}"
fi

wget \${product_staging_properties_url} -O \${product_staging_properties_name}

echo -e "import sys,os,re
from urllib2 import urlopen
ret=0
def isvalidurl(url, inc_str):
    print url
    try:
        code = urlopen(url).code
    except IOError:
        print 'ERROR ', url + ' is invalid!'
        ret=1
        return 1
    if (code / 100 >= 4):
        print 'ERROR ', url + ' is invalid!'
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

def validateProperties(propfile, keyword, product_name):
    dic = {}
    if os.path.isfile(propfile):
        tmpFile = open(propfile, 'r')
        for line in tmpFile:
            str1, tmp, str2 = line.partition('=')
            str2 = str2.replace('\\\\\\\\n', '')
            dic[str1] = str2
        tmpFile.close()
        if re.match('rhdm-.*', propfile) is not None:
            isvalidurl(dic['rhdm.addons.latest.url'],keyword)
            isvalidurl(dic['rhdm.kie-server.ee7.latest.url'],keyword)
            isvalidurl(dic['rhdm.decision-central.standalone.latest.url'],keyword)
            isvalidurl(dic['rhdm.decision-central-eap7.latest.url'],keyword)
            isvalidurl(dic['rhdm.installer.latest.url'],keyword)
            isvalidurl(dic['build.config'],'bxms')

            if '\${release_code}' != 'bxms-nightly':
                isvalidurl(dic['rhdm.maven.repo.latest.url'],keyword)
                isvalidurl(dic['rhdm.sources.latest.url'],keyword)

            assertEqual('\$kie_version', dic['KIE_VERSION'])
            assertEqual('\${product1_artifact_version}', dic['RHDM_VERSION'])
            assertContain(dic['rhdm.decision-central.standalone.latest.url'], '\$product1_milestone_version')
            assertContain(dic['rhdm.addons.latest.url'], '\$product1_milestone_version')
            assertContain(dic['rhdm.kie-server.ee7.latest.url'], '\$product1_milestone_version')
            assertContain(dic['rhdm.installer.latest.url'], '\$product1_milestone_version')

        if re.match('rhba-.*', propfile) is not None:
            isvalidurl(dic['rhba.addons.latest.url'],keyword)
            isvalidurl(dic['rhba.kie-server.ee7.latest.url'],keyword)
            isvalidurl(dic['rhba.decision-central.standalone.latest.url'],keyword)
            isvalidurl(dic['rhba.decision-central-eap7.latest.url'],keyword)
            isvalidurl(dic['rhba.installer.latest.url'],keyword)
            isvalidurl(dic['build.config'],'bxms')

            if '\${release_code}' != 'bxms-nightly':
                isvalidurl(dic['rhba.maven.repo.latest.url'],keyword)
                isvalidurl(dic['rhba.sources.latest.url'],keyword)
            assertEqual('\$kie_version', dic['KIE_VERSION'])
            assertEqual('\${product2_artifact_version}', dic['RHBAS_VERSION'])
            assertContain(dic['rhba.business-central.standalone.latest.url'], '\$product2_milestone_version')
            assertContain(dic['rhba.addons.latest.url'], '\$product2_milestone_version')
            assertContain(dic['rhba.kie-server.ee7.latest.url'], '\$product2_milestone_version')
            assertContain(dic['rhba.installer.latest.url'], '\$product2_milestone_version')

        if ret != 0:
            print propfile + ' Validation No Pass'
            sys.exit(1)
        else:
            print  propfile + ' Validation Pass'
    else:
        return 1
print '---Exec the py script...---'
validateProperties('\${product_staging_properties_name}', 'rcm-guest','\${PRODUCT_NAME}')
">validateProperties.py
python validateProperties.py
"""
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-validate-qe-properties") {

    // Sets a description for the job.
    description("This job is responsible for uploading release to candidate area.")
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
    if(JOB_NAME.matches("codereview/(.*)")){
        println "Detected in codereview:Disable validate-qe-properties"
        disabled()
    }

    // Allows to parameterize the job.
    parameters {
        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "RHDM",
                description = "Specify product name to switch between configurations.")
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
                triggers/'gerritProjects'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'/'filePaths'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' << {
                    'compareType' 'REG_EXP'
                    'pattern' 'stream/bxms/release-history/*-handover.adoc'
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
