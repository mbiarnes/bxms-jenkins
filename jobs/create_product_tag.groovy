import org.jboss.bxms.jenkins.JobTemplate

// Create product tag script
String shellScript = """
#unset Jenkins WORKSPACE variable to avoid clash with ip-tooling
unset WORKSPACE

echo -e \"Host code.engineering.redhat.com \\n\\
        HostName code.engineering.redhat.com \\n\\
        User jb-ip-tooling-jenkins\" > ~/.ssh/config
chmod 600 ~/.ssh/config
tag_version=\${product_name}-\${product_version}\${availability}.\${release_milestone}
MVN_LOCAL_REPO=/jboss-prod/m2/bxms-dev-repo RELEASE_TAG=\${tag_version} LOCAL=1 CFG=./${IP_CONFIG_FILE} \
    REPO_GROUP=MEAD make POMMANIPEXT=bxms-bom -f \${makefile} \${product1_lowcase} \${product2_lowcase} 2>&1

sed -i '/^product_tag=/d' ${CI_PROPERTIES_FILE} && echo \"product_tag=\${tag_version}\" >> ${CI_PROPERTIES_FILE}

#need to verify if all tags are created succesfully
EXIST_MISSING_TAG=0
echo \"Verifying \${tag_version} tag...\"

#extract all tag locations from the log file
cat ${IP_CONFIG_FILE} | grep -Eo \"https://code.engineering.redhat.com.*\\.git\"| awk -F\"/\" '{print \$5\"/\"\$6}' | grep -Eo \".*\\.git\" > tags_location.txt

while read -r line;do
   # curl the tag url; if find return HTTP/1.1 200 OK; if not,return HTTP/1.1 404 Not found
   curl -Is \"http://git.app.eng.bos.redhat.com/git/\${line}/tag/?h=\${tag_version}\" | head -n 1 > curl_result
   if grep -q \"404\" curl_result;then
      echo \"Missing \${tag_version} tag in \${line}. Please perform some checking...\"
      EXIST_MISSING_TAG=1
   fi
done < tags_location.txt

#clear temp files
rm tags_location.txt
rm curl_result

#print result
if [ \${EXIST_MISSING_TAG} -eq 0 ]; then
   echo \"All tags have been successfully found\"
else
   echo \"Failed to create some tags\"
   exit 1
fi
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-create-product-tag") {

    // Sets a description for the job.
    description("This job is responsible for creating the product milestone tags for this release in the format of ProductVersion.Milestone.")

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

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, "bxms-nightly")
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
