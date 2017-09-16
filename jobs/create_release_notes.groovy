import org.jboss.bxms.jenkins.JobTemplate

// Generate html release notes
def shellScript ='''#!/bin/sh
#copy the template 
cp ./\${release_prefix}-release/\${release_prefix}-notes-template.html \${bxms_release_notes_path}

jql_cve_search="(project = RHBRMS OR project = RHBPMS) AND 'Target Release' = ${product_version}.GA AND labels = security AND Status = closed"
jql_bugfix_search="(project = RHBRMS OR project = RHBPMS) AND 'Target Release' = ${product_version}.GA AND status = VERIFIED"

echo "cve search link: Please click the following link"
echo "https://issues.jboss.org/issues/?jql=${jql_cve_search}"|sed -e "s/ /%20/g"| sed -e "s/'/%22/g"
echo "bug fix search link: Please click the following link"
echo "https://issues.jboss.org/issues/?jql=${jql_bugfix_search}"|sed -e "s/ /%20/g"| sed -e "s/'/%22/g"

./ip-tooling/release-ticketor.py --jql "$jql_cve_search" $product_version.GA $cutoff_date $product_version 2>&1 | tee ./jql_search_data.txt

#Clear repeating lines
cat jql_search_data.txt|awk \'!n[\$0]++\'|tee jql_search_data.txt


while read -r line; do
   table_1=$(echo "$line" |awk -F"- " \'{ print "<tr><td><a href=\\\"https://access.redhat.com/security/cve/\"$1\"\\\">"$1"</a></td><td>\" $2\"</td></tr>\\n"}\')
   sed -i "/<!--table_1_appending_mark-->/ a $table_1" ${bxms_release_notes_path}
done < jql_search_data.txt


./ip-tooling/release-ticketor.py --jql "$jql_bugfix_search" $product_version.GA $cutoff_date $product_version 2>&1 | tee ./jql_search_data.txt

while read -r line; do
   table_2=$(echo "$line" |awk -F"- " \'{ print "<tr><td><a href=\\\"https://issues.jboss.org/browse/\"$1\"\\\">"$1"</a></td><td>\"$2\"</td></tr>\\n"}\')
   sed -i "/<!--table_2_appending_mark-->/ a $table_2" ${bxms_release_notes_path}
done < ./jql_search_data.txt

sed -i "s/\\${kie_version}/$kie_version/g" ${bxms_release_notes_path}
sed -i "s/\\${product_artifact_version}/$product_artifact_version/g" ${bxms_release_notes_path}
sed -i "s/\\${product_version}/$product_version/g" ${bxms_release_notes_path}
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-notes") {

    // Sets a description for the job.
    description("This job is responsible for generating a html release description.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
