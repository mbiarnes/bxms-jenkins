package org.Jboss.bxms.jenkins

import hudson.model.Item
import hudson.model.View
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.GeneratedJob
import javaposse.jobdsl.dsl.GeneratedView
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.plugin.JenkinsJobManagement
import jenkins.model.Jenkins
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests that all dsl scripts in the jobs directory will compile. All config.xml's are written to build/debug-xml.
 */
class JobScriptsSpec extends Specification {
    // add in a sample environment for testing
    def enviMap = [
    'RELEASE_CODE':'rhdm-71-test',
    'pipelineSeqFile':'release_pipeline_seq.cfg',
    'CI_PROPERTIES_FILE':'rhdm-test-jenkins-ci.properties',
    'IP_CONFIG_FILE':'rhdm-test.cfg',
    'JOB_NAME':'a-master-seed',
    'GERRIT_BRANCH':'master',
    'GERRIT_REFSPEC':'refs/heads/master',
    'OPENSTACK_PUBLIC_IP': '127.0.0.1'
    ]

    @Shared
    @ClassRule
    private JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared
    private File outputDir = new File('./build/debug-xml')

    def setupSpec() {
        outputDir.deleteDir()
    }

    @Unroll
    void 'test script #file.name'(File file) {

        given:
        JobManagement jm = new JenkinsJobManagement(System.out, enviMap, new File('.'))

        when:
        GeneratedItems items = new DslScriptLoader(jm).runScript(file.text)
        writeItems items

        then:
        noExceptionThrown()

        where:
        file << jobFiles
    }

    /**
     * Write the config.xml for each generated job and view to the build dir.
     */
    void writeItems(GeneratedItems items) {
        Jenkins jenkins = jenkinsRule.jenkins

        items.jobs.each { GeneratedJob generatedJob ->
            String jobName = generatedJob.jobName
            Item item = jenkins.getItemByFullName(jobName)
            String text = new URL(jenkins.rootUrl + item.url + 'config.xml').text
            writeFile new File(outputDir, 'jobs'), jobName, text
        }

        items.views.each { GeneratedView generatedView ->
            String viewName = generatedView.name
            View view = jenkins.getView(viewName)
            String text = new URL(jenkins.rootUrl + view.url + 'config.xml').text
            writeFile new File(outputDir, 'views'), viewName, text
        }
    }

    /**
     * Write a single XML file, creating any nested dirs.
     */
    void writeFile(File dir, String name, String xml) {
        List tokens = name.split('/')
        File folderDir = tokens[0..<-1].inject(dir) { File tokenDir, String token ->
            new File(tokenDir, token)
        }
        folderDir.mkdirs()

        File xmlFile = new File(folderDir, "${tokens[-1]}.xml")
        xmlFile.text = xml
    }

    static List<File> getJobFiles() {
        List<File> files = []
        files << new File('jobs/a_master_seed.groovy')

    }
}
