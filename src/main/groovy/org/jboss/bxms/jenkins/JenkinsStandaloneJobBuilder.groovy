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
        def bomSource = "POMMANIPEXT=\${product_lowercase}-build-bom"
        if (release_code.contains('-da')) {
            bomSource = ''
        }

        sections.each { section_name, section ->
            if ((!section.containsKey("config_type")) || (section.containsKey("config_type") && section.get("config_type").equals("bom-builder")) ) {
                String shellScript = """
set +e
# set up dir for deployment cache and make sure it exists
DEPLOY_DIR=\$WORKSPACE/deployDirectory
mkdir -p \$DEPLOY_DIR

unset WORKSPACE
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
#Only debug purpose
#cp /jboss-prod/config/rhpam-master-nightly-dev.cfg .

JOB_SETTINGS_XML=/tmp/\${product_lowercase}-\${product_version_major}\${product_version_minor}-\${JOB_BASE_NAME}-settings.xml
cat <<EOT > \$JOB_SETTINGS_XML
<?xml version="1.0" encoding="UTF-8"?>
<!--
 Copyright 2017 Red Hat, Inc, and individual contributors.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <profiles>
    <profile>
      <id>nighlty-repo</id>
      <activation>
          <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
          <altDeploymentRepository>local::default::file://\$DEPLOY_DIR</altDeploymentRepository>
        </properties>
      <repositories>
        <!-- RHBA Nightly repo -->
        <repository>
          <id>shared-imports</id>
          <url>http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-master-nightly</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>shared-imports</id>
          <url>http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-master-nightly</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <servers>
  <server>
    <id>nexus-release</id>
    <username>kieciuser</username>
    <password>jbo55.rocks!</password>
    <configuration>
      <httpConfiguration>
        <all>
          <useDefaultHeaders>false</useDefaultHeaders>
          <headers>
            <header>
              <name>Cache-control</name>
              <value>no-cache</value>
            </header>
            <header>
              <name>Cache-store</name>
              <value>no-store</value>
            </header>
            <header>
              <name>Pragma</name>
              <value>no-cache</value>
            </header>
            <header>
              <name>Expires</name>
              <value>0</value>
            </header>
          </headers>
        </all>
      </httpConfiguration>
    </configuration>
  </server>
</servers>
  <activeProfiles>
    <activeProfile>nighlty-repo</activeProfile>
  </activeProfiles>
</settings>
EOT
#Patch the MEAD_simulator.sh for preventing run hudson archive and deploy check
sed -i 's/cd "\$_ARCHIVE"/exit \$_ERR;cd "\$_ARCHIVE"/' ip-tooling/MEAD_simulator.sh
if [ ! -z \${build_date} ]; then
    sed -i "s#-SNAPSHOT#-\${build_date}#g" rh*-dev.cfg
    sed -i -E "s#(upstream_tag=.*)#\\1,`date -u -d \${build_date} +'%Y-%m-%d'`#g" rh*-dev.cfg
fi
if [ "${job_type}" == "nightly" ]; then
    sed -i "s#ip.config.sha=#cfg.url.template=file://`pwd`/{0},ip.config.sha=#g" ${cfg_filename}
    #SkipTests except every Saturday
    if [ `date +%w` != 6 ] && [ "\${SKIPTEST}" == "true" ]; then
      sed -i "s#^mvnSkipTestsOption=#mvnSkipTestsOption=skipTests=true,#g" ${cfg_filename}
    fi
fi
ln -sf `pwd`/workspace/build.${section_name}/.m2 /tmp/\${product_lowercase}\${product_version_major}\${product_version_minor}.${section_name}
let retry=3
while [ \$retry -ne 0 ]; do
    MVN_DEP_REPO=local::default::file://\$DEPLOY_DIR \
    MVN_LOCAL_REPO=/tmp/\${product_lowercase}\${product_version_major}\${product_version_minor}.${section_name} \
    MVN_SETTINGS=\$JOB_SETTINGS_XML \
    LOCAL=1 CFG=${cfg_filename} ${bomSource} make DEBUG=\$DEBUG ${section_name}
    ret=\$?
    #Retry if hit DA rest call timeout error, it will skip automatically retry if build not depends on DA services
    grep "REST client finished with failures..." "workspace/build.${section_name}/mvn.log"
    if [ \$? -eq 0 ]; then
        let retry-=1
        sleep 15
    else
    # Retry 1 time only because network issues during maven repository fetching
        if [ \$retry -lt 3 ];then
          break
        fi
        let retry-=1
    fi
done

# unpack zip to QA Nexus
cd \$DEPLOY_DIR
zip -r kiegroup .
curl --upload-file kiegroup.zip -u \$kieUnpack -v http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/service/local/repositories/scratch-release-rhba-master/content-compressed
cd ..

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
                        booleanParam('SKIPTEST',true,'Remove check if you do not want to skip Unittest and integration test')
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

                        credentialsBinding {
                            usernamePassword("kieUnpack" , "unpacks-zip-on-qa-nexus")
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
