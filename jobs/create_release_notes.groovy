import org.jboss.bxms.jenkins.JobTemplate

// Generate html release notes (Note that don't replace ''')
def shellScript ='''#!/bin/sh
bpms_jql_cve_search="(project = RHBRMS OR project = RHBPMS) AND 'Target Release' = ${product_version}.GA AND labels = security AND (Status = closed or Status = VERIFIED)"
bpms_jql_bugfix_search="(project = RHBRMS OR project = RHBPMS) AND 'Target Release' = ${product_version}.GA AND (status = VERIFIED or status = closed) AND summary !~ 'CVE*'"

brms_jql_cve_search="(project = RHBRMS OR project = RHBPMS) AND 'Target Release' = ${product_version}.GA AND labels = security AND (status = VERIFIED or status = closed) AND component not in ('Form Modeler', 'jBPM Core', 'jBPM Designer') "
brms_jql_bugfix_search="(project = RHBRMS OR project = RHBPMS) AND 'Target Release' = ${product_version}.GA AND (status = VERIFIED or status = closed) AND component not in ('Form Modeler', 'jBPM Core', 'jBPM Designer') AND summary !~ 'CVE*'"

kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
function generate_release_notes () {
    #Replace the variable from properties
    python ip-tooling/template_helper.py -i ./\${release_prefix}-release/\${release_prefix}-release-notes.template -p \${ci_properties_file} -o \$1
    
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

generate_release_notes ${brms_release_notes_path} "${brms_jql_cve_search}" "${brms_jql_bugfix_search}"
generate_release_notes ${bpms_release_notes_path} "${bpms_jql_cve_search}" "${bpms_jql_bugfix_search}"

'''

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-release-notes") {

    // Sets a description for the job.
    description("This job is responsible for generating a html release description.")

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
                    sourceFiles('${brms_release_notes_path}')

                    // Sets the destination folder.
                    remoteDirectory('${brms_staging_path}/')
                }
                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('${bpms_release_notes_path}')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_staging_path}/')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, RELEASE_CODE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
