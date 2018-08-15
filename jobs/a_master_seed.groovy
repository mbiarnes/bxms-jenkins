import org.jboss.bxms.jenkins.*

//Establish the parametize release pipeline
//Release code is identical to the folder name in streams/
def currentJobName="${JOB_NAME}"
def gerritRefspec="+refs/heads/master:refs/remotes/origin/master"
// this is a git point to the refs we used in gerritRefspec which git would fetched
def gerritBranch="master"
if (currentJobName.matches("(.*)codereview/(.*)")) {
    println "Detected in codereview folder, reset GERRIT_REFSPEC/GERRIT_BRANCH:"
    gerritBranch ="FETCH_HEAD"
    // if triggered by manul, to avoid build fail
    try{
        gerritRefspec="${GERRIT_REFSPEC}"
    }catch(e){
        println "Detected triggered by manual: set GERRIT_REFSPEC to '+refs/heads/master:refs/remotes/origin/master'"
        gerritRefspec="+refs/heads/master:refs/remotes/origin/master"
    }
}
try{
    println "-----OPENSTACK_PUBLIC_IP: "+OPENSTACK_PUBLIC_IP+ "-----"
}catch(e){
    println "-----OPENSTACK_PUBLIC_IP: UnKnow. This job may not running on any openstack instance. -----"
}
println "-------currentJobName:${currentJobName}-------"
println "-------GERRIT_BRANCH:${gerritBranch}-------"
println "-------GERRIT_REFSPEC:${gerritRefspec}-------"
if(currentJobName == null || gerritRefspec == null || gerritBranch == null ){
    throw new javaposse.jobdsl.dsl.DslException("The JOB_NAME/GERRIT_BRANCH/GERRIT_REFSPEC parameters is not setting!Exit...");
}
def ReleasePipelineBuilder(_release_code, _cfg_file, _properties_file, gerritBranch , gerritRefspec , currentJobName, cron_val = null) {
    new ReleaseSingleJobBuilder(
        release_code: _release_code,
        cfg_file:_cfg_file,
        ci_properties_file:_properties_file,
        gerritBranch: gerritBranch,
        gerritRefspec: gerritRefspec,
        jobName:currentJobName
    ).build(this)
    new ReleasePipelineJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            ci_properties_file:_properties_file,
            cron_val: cron_val,
            gerritBranch: gerritBranch,
            gerritRefspec: gerritRefspec,
            jobName:currentJobName
    ).build(this)
}

ReleasePipelineBuilder("rhdm-70", "rhdm-70.cfg", "/jboss-prod/config/rhdm-70-ci.properties",gerritBranch , gerritRefspec,currentJobName )
ReleasePipelineBuilder("rhdm-71-nightly", "rhdm-71-dev.cfg", "/jboss-prod/config/rhdm-71-nightly-ci.properties", gerritBranch , gerritRefspec,currentJobName)
//ReleasePipelineBuilder("rhdm-71-test", "rhdm-71-test.cfg", "/jboss-prod/config/rhdm-71-test-ci.properties",gerritBranch , gerritRefspec,currentJobName)

ReleasePipelineBuilder("rhpam-70", "rhpam-70.cfg", "/jboss-prod/config/rhpam-70-ci.properties",gerritBranch , gerritRefspec,currentJobName )
//ReleasePipelineBuilder("rhpam-70-nightly", "rhpam-70-dev.cfg", "/jboss-prod/config/rhpam-70-nightly-ci.properties", gerritBranch , gerritRefspec,currentJobName, "H 17 * * *" )
ReleasePipelineBuilder("rhpam-71-nightly", "rhpam-71-dev.cfg", "/jboss-prod/config/rhpam-71-nightly-ci.properties", gerritBranch , gerritRefspec,currentJobName, "H 17 * * *" )
ReleasePipelineBuilder("rhpam-70-test", "rhpam-70-test.cfg", "/jboss-prod/config/rhpam-70-test-ci.properties",gerritBranch , gerritRefspec,currentJobName)
//ReleasePipelineBuilder("rhpam-70-da-nightly", "rhpam-70-da-dev.cfg", "/jboss-prod/config/rhpam-70-da-nightly-ci.properties", gerritBranch , gerritRefspec,currentJobName)

//Release code is identical to the folder name in streams/
def JenkinsStandaloneJobsBuilder(_release_code, _properties_file, _cfg_file, _job_type, gerrit_ref_spec=''){
    new JenkinsStandaloneJobBuilder(
            release_code: _release_code,
            ci_properties_file:_properties_file,
            cfg_file:_cfg_file,
            job_type: _job_type,
            gerrit_ref_spec: gerrit_ref_spec
    ).build(this)

    new JenkinsAllJobBuilder(
            release_code: _release_code,
            ci_properties_file:_properties_file,
            cfg_file:_cfg_file,
            job_type: _job_type
    ).build(this)

    new JenkinsAllJobBuilderPipeline(
            release_code: _release_code,
            cfg_file:_cfg_file,
            job_type: _job_type,
            gerrit_ref_spec: gerrit_ref_spec
    ).build(this)
}
JenkinsStandaloneJobsBuilder("rhdm-71", "/jboss-prod/config/rhdm-71-nightly-ci.properties", "rhdm-71-dev.cfg", "nightly" )
//JenkinsStandaloneJobsBuilder("rhpam-70", "/jboss-prod/config/rhpam-70-nightly-ci.properties", "rhpam-70-dev.cfg", "nightly" )
//JenkinsStandaloneJobsBuilder("rhpam-70-da", "/jboss-prod/config/rhpam-71-da-nightly-ci.properties", "rhpam-70-da-dev.cfg", "nightly")
JenkinsStandaloneJobsBuilder("rhpam-71", "/jboss-prod/config/rhpam-71-nightly-ci.properties", "rhpam-71-dev.cfg", "nightly" )

def dirNameRow=["codereview","utility", "rzhang_coderreview"]
//if you want to create codereviewer(rzhang)'s directory and his master seed job,do like this:
//def dirNameRow=["codereview","utility","rzhang_coder_review"]
new CodeReviewJobBuilder(
        dirNameRow: dirNameRow,
        jobName:currentJobName
).build(this)

//new TestPerformanceJobBuilder().build(this)
