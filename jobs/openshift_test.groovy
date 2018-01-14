import org.jboss.bxms.jenkins.JobTemplate
String shellScript = '''
    sudo oc cluster down || /bin/true
    echo -e "\\n-------\\n$(sudo oc cluster status)\\n--------\\n"
    echo -e "\\n-------\\nnode public-ip:${OPENSTACK_PUBLIC_IP}\\n--------\\n"
    sudo oc cluster up --public-hostname="${OPENSTACK_PUBLIC_IP}"
    oc login localhost:8443 -u developer -p developer --insecure-skip-tls-verify=true
    oc new-project "${OCPROJECTNAME}" || /bin/true
    oc project "${OCPROJECTNAME}"
    oc policy add-role-to-user admin admin -n "${OCPROJECTNAME}"
    oc policy add-role-to-user admin system:serviceaccount:decisioncentral-service-account:default -n "${OCPROJECTNAME}"
    oc policy add-role-to-user view system:serviceaccount:kieserver-service-account:default -n "${OCPROJECTNAME}"
    oc create -f kieserver-app-secret.yaml
    oc create -f decisioncentral-app-secret.yaml
    #wget image-streams is for fixing the imagestream bug, this won't be needed after my PR is merged
    wget https://raw.githubusercontent.com/ryanzhang/rhdm-7-openshift-image/a8f10a7f4a33dfc3becea3e5e3d7d7107ded109b/rhdm70-image-streams.yaml -O rhdm70-image-streams.yaml

    #Replace the docker image stream from rhcc to brew stage registry since ER image only  availabe in staging folder
    sed -i 's/registry.access.redhat.com/brew-pulp-docker01.web.prod.ext.phx2.redhat.com:8888/g' rhdm70-image-streams.yaml
    oc create -f rhdm70-image-streams.yaml

    oc process -n "${OCPROJECTNAME}" -f templates/rhdm70-full.yaml -p IMAGE_STREAM_NAMESPACE="${OCPROJECTNAME}" -p ADMIN_PASSWORD=admin\\! -p KIE_ADMIN_USER=adminUser -p KIE_ADMIN_PWD=admin1\\! -p KIE_SERVER_CONTROLLER_USER=controllerUser -p KIE_SERVER_CONTROLLER_PWD=controller1\\! -p KIE_SERVER_USER=executionUser -p KIE_SERVER_PWD=execution1\\! |oc create -n "${OCPROJECTNAME}" -f -

'''
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-openshift-test") {

    // Sets a description for the job.
    description("This job is responsible for test openshift images.")
    scm {

        github("jboss-container-images/rhdm-7-openshift-image")

    }
    parameters{
        stringParam('OCPROJECTNAME', 'myproject', 'the project name to build in openshift')
    }
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
    label("openshift-test")

}

//JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
