import org.jboss.bxms.jenkins.JobTemplate

def shell_script = """

# Workaround for variable name conflict between Jenkins and ip-tooling
unset WORKSPACE
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
echo "Validating upstreams in ${IP_CONFIG_FILE}"
VALIDATE_ONLY=true LOCAL=1 CFG=./${IP_CONFIG_FILE} REPO_GROUP=MEAD+JENKINS+JBOSS+CENTRAL MVN_LOCAL_REPO=/jboss-prod/m2/\${dev_maven_repo} POMMANIPEXT=bxms-bom make -f Makefile.BRMS rhdm-installer rhbas-installer
"""

def jobDefinition = job("${RELEASE_CODE}-validate-build-config") {
    description("Validate if upstream source configuration is proper")

    label('bxms-nightly')

    // build steps
    steps {
        shell(shell_script)
    }
    // clear workspace
    wrappers {
        preBuildCleanup()
    }

}
JobTemplate.addIpToolingScmConfiguration(jobDefinition,GERRIT_BRANCH , GERRIT_REFSPEC)
