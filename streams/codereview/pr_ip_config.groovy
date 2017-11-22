def shell_script = """
# Workaround for variable name conflict between Jenkins and ip-tooling 
unset WORKSPACE

changed_cfgs=`git diff --name-only HEAD HEAD~1 | grep -i ^bxms.\\*\\.cfg`
for cfg in \${changed_cfgs[*]}
do
    echo "Changes found in \${cfg}, validating..."
    VALIDATE_ONLY=true LOCAL=1 REPO_GROUP=MEAD+JENKINS+JBOSS+CENTRAL CFG=./\${cfg} MVN_LOCAL_REPO=/jboss-prod/m2/bxms-dev-repo POMMANIPEXT=bxms-bom make -f Makefile.BRMS rhdm-installer rhbas-installer
done
"""

job('bxms-ip-config-codereview'){
    description("Monitor the code change in integration-platform-config")

    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "GERRIT_REFSPEC", defaultValue = "refs/heads/master", description = "Parameter passed by Gerrit code review trigger")
        stringParam(parameterName = "GERRIT_BRANCH", defaultValue = "master", description = "Parameter passed by Gerrit code review trigger")

    }
    multiscm {
        // Adds a Git SCM source.
        git {
            // Adds a remote.
            remote {
                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                name("origin")
                refspec("+refs/heads/*:refs/remotes/origin/* \$GERRIT_REFSPEC")

            }
            // Specify the branches to examine for changes and to build.
            branch("\$GERRIT_BRANCH")
            extensions {
                choosingStrategy {
                    gerritTrigger()
                }

            }


        }
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling")
            }

            // Specify the branches to examine for changes and to build.
            branch("master")

            // Adds additional behaviors.
            extensions {

                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                relativeTargetDirectory('ip-tooling')
            }
        }
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
                   'pattern' 'bxms*.cfg'
               }
           }
       }
   }
   label('bxms-nightly')
   
   // build steps 
   steps{
       shell(shell_script)
    }
   // clear workspace 
    wrappers {
        preBuildCleanup()
    }
}

