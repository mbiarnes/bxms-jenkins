import org.jboss.bxms.jenkins.*

//Establish the parametize release pipeline
//Release code is identical to the folder name in streams/
def seedJobName="${JOB_NAME}"
def gerritRefspec="+refs/heads/master:refs/remotes/origin/master"
// this is a git point to the refs we used in gerritRefspec which git would fetched
def gerritBranch="master"
if (seedJobName.matches("codereview/(.*)")) {
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
println "-------seedJobName:${seedJobName}-------"
println "-------GERRIT_BRANCH:${gerritBranch}-------"
println "-------GERRIT_REFSPEC:${gerritRefspec}-------"
if(seedJobName == null || gerritRefspec == null || gerritBranch == null ){
    throw new javaposse.jobdsl.dsl.DslException("The JOB_NAME/GERRIT_BRANCH/GERRIT_REFSPEC parameters is not setting!Exit...");
}
def ReleasePipelineBuilder(_release_code, _cfg_file, _properties_file, gerritBranch , gerritRefspec , seedJobName, cron_val = null) {
    new ReleaseSingleJobBuilder(
        release_code: _release_code,
        cfg_file:_cfg_file,
        ci_properties_file:_properties_file,
        gerritBranch: gerritBranch,
        gerritRefspec: gerritRefspec,
        jobName:seedJobName
    ).build(this)
    new ReleasePipelineJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            ci_properties_file:_properties_file,
            cron_val: cron_val,
            gerritBranch: gerritBranch,
            gerritRefspec: gerritRefspec,
            jobName:seedJobName
    ).build(this)
}

ReleasePipelineBuilder("rhdm", "rhdm.cfg", "/jboss-prod/config/rhdm-ci.properties",gerritBranch , gerritRefspec,seedJobName )
ReleasePipelineBuilder("rhdm-test", "rhdm-test.cfg", "/jboss-prod/config/rhdm-test-ci.properties",gerritBranch , gerritRefspec,seedJobName )
ReleasePipelineBuilder("rhdm-nightly", "rhdm-dev.cfg", "/jboss-prod/config/rhdm-nightly-ci.properties", gerritBranch , gerritRefspec,seedJobName, "H 17 * * *" )
ReleasePipelineBuilder("rhba-nightly", "rhba-dev.cfg", "/jboss-prod/config/rhba-nightly-ci.properties", gerritBranch , gerritRefspec,seedJobName, "H 17 * * *" )

//Release code is identical to the folder name in streams/
def JenkinsStandaloneJobsBuilder(_release_code, _properties_file, _cfg_file, _job_type){
    new JenkinsStandaloneJobBuilder(
            release_code: _release_code,
            ci_properties_file:_properties_file,
            cfg_file:_cfg_file,
            job_type: _job_type
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
            job_type: _job_type
    ).build(this)
}
JenkinsStandaloneJobsBuilder("rhdm", "/jboss-prod/config/rhdm-nightly-ci.properties", "rhdm-dev.cfg", "nightly" )
JenkinsStandaloneJobsBuilder("rhba", "/jboss-prod/config/rhba-nightly-ci.properties", "rhba-dev.cfg", "nightly" )

def dirNameRow=["codereview","utility"]
//if you want to create codereviewer(rzhang)'s directory and his master seed job,do like this:
//def dirNameRow=["codereview","utility","rzhang_coder_review"]
new GeneralSeedJobBuilder(
        dirNameRow: dirNameRow,
        jobName:seedJobName
).build(this)
