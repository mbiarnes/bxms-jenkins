import org.jboss.bxms.jenkins.JobTemplate

String shellScript = """
function getGroupId()
{
  local _input=\$1
  if echo \$_input |grep ".*:.*:.*:.*" 2>&1 1>/dev/null ;then
    echo \$_input |sed "s/\\(.*\\):\\(.*\\):\\(.*\\):\\(.*\\)/\\1/g"
    return 0
  fi

  if echo \$_input |grep ".*:.*:.*" 2>&1 1>/dev/null ;then
    echo \$_input |sed "s/\\(.*\\):\\(.*\\):\\(.*\\)/\\1/g"
    return 0
  fi

  if echo \$_input |grep ".*:.*" 2>&1 1>/dev/null ;then
    echo \$_input |sed "s/\\(.*\\):\\(.*\\)/\\1/g"
    return 0
  fi

  if echo \$_input |grep ".*-.*-.*-.*" 2>&1 1>/dev/null ;then
    local BUILDINFO=`brew buildinfo \$_input`
    echo "\$BUILDINFO" | awk '/Maven groupId:/ {print \$3}'
  fi

}

function getArtifactId()
{
  if echo \$1 |grep ".*:.*:.*:.*" 2>&1 1>/dev/null ;then
    echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\):\\(.*\\)/\\2/g"
    return 0
  fi

  if echo \$1 |grep ".*:.*:.*" 2>&1 1>/dev/null ;then
    echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\)/\\2/g"
    return 0
  fi

  if echo \$1 |grep ".*:.*" 2>&1 1>/dev/null ;then
    echo \$1 |sed "s/\\(.*\\):\\(.*\\)/\\2/g"
    return 0
  fi

  if echo \$1 |grep ".*-.*-.*-.*" 2>&1 1>/dev/null ;then
    local BUILDINFO=`brew buildinfo \$1`
    echo "\$BUILDINFO" | awk '/Maven artifactId:/ {print \$3}'
  fi
}

function getVersion()
{
  if echo \$1 |grep ".*:.*:.*:.*" 2>&1 1>/dev/null ;then
    echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\):\\(.*\\)/\\4/g"
    return 0
  fi

  if echo \$1 |grep ".*:.*:.*" 2>&1 1>/dev/null ;then
    echo \$1 |sed "s/\\(.*\\):\\(.*\\):\\(.*\\)/\\3/g"
    return 0
  fi

  if echo \$1 |grep ".*-.*-.*-.*" 2>&1 1>/dev/null ;then
    local BUILDINFO=`brew buildinfo \$1`
    echo "\$BUILDINFO" | awk '/Maven version:/ {print \$3}'
  fi
}

function importToMead()
{
    if [ ! -e /tmp/utility-scripts ] ;then
        echo Checking out utility-scripts...
        UTILITY_URL="git://git.app.eng.bos.redhat.com/rcm/utility-scripts.git"
        git clone \$UTILITY_URL /tmp/utility-scripts &> /dev/null
    fi
    PATH=/tmp/utility-scripts/mead:/tmp/utility-scripts:\$PATH

    local _artifact="\$1"
    while [ -n "\$1" ] ;do
        local _artifact="\$(echo \$1 |tr -d ',')"
        shift
        local g=\$(getGroupId \$_artifact)
        local a=\$(getArtifactId \$_artifact)
        local v=\$(getVersion \$_artifact)
        local gavpath=\$(echo \$(echo "\$g" | sed "s|\\.|/|g")/\$a/\$v)
        local _mavenrepo="/jboss-prod/m2/\${jenkins_cache_repo}"
        local _importtag=\$brew_importtag
        local _importowner="bxms-release/prod-ci"
        echo ":) Importing \$g:\$a:\$v into \$_importtag by \$_importowner"
        import-maven --owner=\$_importowner --tag=\$_importtag \$(find \$_mavenrepo/\$gavpath  -name '*.jar' -o -name '*.pom')
        if [ \$? -ne 0 ] ;then
            mkdir -p \$_mavenrepo/\$gavpath
            cd \$_mavenrepo/\$gavpath
            get-maven-artifacts \$g:\$a:\$v
            import-maven --owner=\$_importowner --tag=\$_importtag *
            if [ \$? -ne 0 ] ;then
                echo ":| Failed to import \$_artifact"
            fi
        fi
    done
    return 0
}

function importToMeadFromLog()
{
    [[ ! -f \$1 ]] && echo "\$1 is not existed!" && exit 1

    local importlist="\$(grep ".*:.*:.*" \$1 | sed 's/.* \\([^: ]*:[^: ]*:[^: ]*\\) .*/\\1/')"
    for i in \$importlist ;do
    importToMead \$i
    if [ \$? -ne 0 ] ;then
        echo ":( ERROR Failed to import \$i"
    fi
    done
    echo ""
}

kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
echo "Scanning missing artifact..."
ip-tooling/MEAD_check_artifact.sh \$brew_tag /jboss-prod/m2/\${jenkins_cache_repo} 2>&1 | tee /tmp/mead_check.log
# echo "`tail -n 5 /tmp/mead_check.log`" > /tmp/mead_check.log # For debug purpose
sed -ni "/MISSING/p" /tmp/mead_check.log
sed -i "/redhat-/d" /tmp/mead_check.log
sed -i "/SNAPSHOT/d" /tmp/mead_check.log
importToMeadFromLog /tmp/mead_check.log
echo "JOB DONE"
"""
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-locate-import-list") {

    // Sets a description for the job.
    description("This job is responsible for finding brew missing jars.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)

    }
    publishers {
        postBuildTask {
            //TODO
            task('JOB DONE', "echo 'send an email notification and trigger automation import'")
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition,GERRIT_BRANCH , GERRIT_REFSPEC)
