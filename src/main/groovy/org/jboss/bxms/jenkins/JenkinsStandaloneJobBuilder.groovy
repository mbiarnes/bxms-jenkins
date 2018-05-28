package org.jboss.bxms.jenkins

import ca.szc.configparser.Ini
import javaposse.jobdsl.dsl.DslFactory

/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class JenkinsStandaloneJobBuilder {
    String release_code
    String ci_properties_file
    String job_type
    String cfg_file
    String gerrit_ref_spec

    Map<String, String> maven_repo_map=[
        "rhdm-71":"/jboss-prod/m2/bxms-7.0-", \
        "rhpam-70":"/jboss-prod/m2/bxms-7.0-", \
        "rhpam-70-da":"/jboss-prod/m2/bxms-7.0-da-", \
        "rhpam-71":"/jboss-prod/m2/bxms-7.0-", \
        "rhdm-test":"/jboss-prod/m2/bxms-7.0-"]
    Map<String, String> repo_group_map=["milestone":"MEAD", "nightly":"MEAD+JENKINS+JBOSS+CENTRAL"]
    def build(DslFactory dslFactory) {
        String cfg_filename = cfg_file
        if (cfg_file.contains("/")) {
            String[] cfg_file_paths = cfg_file.tokenize("/")
            cfg_filename = cfg_file_paths[cfg_file_paths.length - 1]
        }
        String urlString = "https://code.engineering.redhat.com/gerrit/gitweb?p=integration-platform-config.git;a=blob_plain;f=" + cfg_filename
        if (gerrit_ref_spec != '')
            urlString = urlString + ";hb=" + gerrit_ref_spec
        BufferedReader configReader = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()))
        Ini _ini_cfg = new Ini().read(configReader)
        Map<String,Map<String,String>> sections = _ini_cfg.getSections()

        dslFactory.folder(release_code + "-" + job_type + "-release-pipeline")
        String maven_repo = maven_repo_map [release_code] + job_type
        String repo_group = repo_group_map [job_type]
        def bomSource = "POMMANIPEXT=\${product_lowercase}-build-bom"
        if (release_code.contains('-da')) {
            bomSource = ''
        }

        sections.each { section_name, section ->
            if ((!section.containsKey("config_type")) || (section.containsKey("config_type") && section.get("config_type").equals("bom-builder")) ) {
                String shellScript = """
set +e
unset WORKSPACE
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
#Only debug purpose
#cp /jboss-prod/config/rhpam-71-dev.cfg .

#Patch the MEAD_simulator.sh for preventing run hudson archive and deploy check
sed -i 's/cd "\$_ARCHIVE"/exit \$_ERR;cd "\$_ARCHIVE"/' ip-tooling/MEAD_simulator.sh
if [ ! -z \${build_date} ]; then
    sed -i "s#-SNAPSHOT#-\${build_date}#g" rh*-dev.cfg
    sed -i -E "s#(upstream_tag=.*)#\\1,`date -u -d \${build_date} +'%Y-%m-%d'`#g" rh*-dev.cfg
fi
if [ "${job_type}" == "nightly" ]; then
    sed -i "s#ip.config.sha=#cfg.url.template=file://`pwd`/{0},ip.config.sha=#g" ${cfg_filename}
fi
let retry=3
while [ \$retry -ne 0 ]; do
    MVN_DEP_REPO=nexus-release::default::file://${maven_repo} REPO_GROUP=${repo_group} LOCAL=1 CFG=${cfg_filename} MVN_LOCAL_REPO=${maven_repo} ${bomSource} make DEBUG=\$DEBUG ${section_name}
    ret=\$?
    grep "REST client finished with failures..." "workspace/build.${section_name}/mvn.log"
    if [ \$? -eq 0 ]; then
        let retry-=1
        sleep 15
    else
        break
    fi
done
exit \$ret
"""
                dslFactory.job(release_code + "-" + job_type + "-release-pipeline/y-" + release_code + "-" + section_name ) {
                    it.description "This job is a seed job for generating " + release_code + " " + job_type + " jenkins build."
                    environmentVariables {
                        // Adds environment variables from a properties file.
                        propertiesFile(ci_properties_file)

                        // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                        keepBuildVariables(true)

                        // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                        keepSystemVariables(true)

                    }
                    logRotator {
                        numToKeep 8
                    }
                    parameters {
                        // Defines a simple text parameter, where users can enter a string value.
                        booleanParam('DEBUG', false, 'Open Debug Log')
                        stringParam('CONFIG_REFS','+refs/heads/master:refs/remotes/origin/master','The refs of integration-platform-config you want to pull,defautl master.')
                        stringParam('TOOLING_REFS','+refs/heads/master:refs/remotes/origin/master','The refs of integration-platform-tooling you want to pull,defautl master.')
                    }

                    if (section.containsKey("jvmOpts".toLowerCase())
                            && (section.get("jvmOpts".toLowerCase()).contains("big"))) {
                        // Groovy gets the original file, so detect BigMem is OK
                        label("nightly-node-bigmemory")
                    } else {
                        label("nightly-node")
                    }

                    multiscm {

                        // Adds a Git SCM source.
                        git {

                            // Adds a remote.
                            remote {

                                // Sets the remote URL.
                                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                                refspec("\$CONFIG_REFS")
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
                                refspec("\$TOOLING_REFS")
                            }

                            // Specify the branches to examine for changes and to build.
                            branch("FETCH_HEAD")

                            // Adds additional behaviors.
                            extensions {

                                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                                relativeTargetDirectory('ip-tooling')
                            }
                        }
                    }
                    wrappers {
                        // Deletes files from the workspace before the build starts.
                        preBuildCleanup(){
                            includePattern('workspace/**')
                            deleteDirectories()
                        }

                    }

                    steps {
                        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                        shell(shellScript)
                    }
                    publishers {
                        archiveJunit("**/TEST-*.xml"){
                            allowEmptyResults(true)
                        }
                        archiveArtifacts{
                            onlyIfSuccessful(false)
                            allowEmpty(true)
                            pattern("**/*.log")
                        }
                        postBuildScripts {
                            steps {
                                shell("echo \"Cleaning worksapce...\" ; rm -rf *")
                            }
                            onlyIfBuildFails(false)
                            onlyIfBuildSucceeds(true)
                        }
                    }
                }
            }
        }
    }
}
