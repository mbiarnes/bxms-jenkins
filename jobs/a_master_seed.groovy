import org.jboss.bxms.jenkins.*

//Establish the parametize release pipeline
//Release code is identical to the folder name in streams/
def seedJobName
def gerritRefspec
// this is a git point to the refs we used in gerritRefspec which git would fetched
def gerritBranch="FETCH_HEAD"

if(GERRIT_REFSPEC){
    gerritRefspec="${GERRIT_REFSPEC}"
}else{
    gerritRefspec="refs/heads/master"
}
if(JOB_NAME){
    seedJobName="${JOB_NAME}"
}else{
    throw new javaposse.jobdsl.dsl.DslException("Job has no name!!!Exit...");
}
if (!seedJobName.matches("(.*)/(.*)")) {
    gerritBranch ="master"
}
println "-------seedJobName:${JOB_NAME}-------"
println "-------GERRIT_BRANCH:${GERRIT_BRANCH}-------"
println "-------jobGERRIT_REFSPEC:${GERRIT_REFSPEC}-------"
if(seedJobName == null || gerritRefspec == null || gerritBranch == null ){
    throw new javaposse.jobdsl.dsl.DslException("The JOB_NAME/GERRIT_BRANCH/GERRIT_REFSPEC parameters is not setting!Exit...");
}
def ReleasePipelineBuilder(_release_code, _cfg_file, _properties_file, gerritBranch , gerritRefspec ,seedJobName , cron_val = null) {
    new ReleasePipelineSeedJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            ci_properties_file:_properties_file,
            gerritBranch: gerritBranch,
            gerritRefspec: gerritRefspec,
            seedJobName: seedJobName,
    ).build(this)

    new ReleasePipelineJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            ci_properties_file:_properties_file,
            cron_val: cron_val,
            gerritBranch: gerritBranch,
            gerritRefspec: gerritRefspec,
    ).build(this)
}

ReleasePipelineBuilder("bxms", "bxms.cfg", "/jboss-prod/config/bxms-ci.properties", gerritBranch , gerritRefspec ,seedJobName )
ReleasePipelineBuilder("bxms-test", "bxms-test.cfg", "/jboss-prod/config/bxms-test-ci.properties",gerritBranch , gerritRefspec ,seedJobName )
ReleasePipelineBuilder("bxms-nightly", "bxms-dev.cfg", "/jboss-prod/config/bxms-nightly-ci.properties", gerritBranch , gerritRefspec,seedJobName, "H 7 * * *" )



//Release code is identical to the folder name in streams/
def JenkinsStandaloneJobsBuilder(_release_code, _cfg_file, _job_type){
    new JenkinsStandaloneJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            job_type: _job_type
    ).build(this)

    new JenkinsAllJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            job_type: _job_type
    ).build(this)

    new JenkinsAllJobBuilderPipeline(
            release_code: _release_code,
            cfg_file:_cfg_file,
            job_type: _job_type
    ).build(this)
}
JenkinsStandaloneJobsBuilder("bxms", "bxms.cfg", "milestone" )
JenkinsStandaloneJobsBuilder("bxms", "/jboss-prod/config/bxms-dev.cfg", "nightly" )

new GeneralSeedJobBuilder(
        release_code: "utility",
        gerritBranch: gerritBranch,
        gerritRefspec: gerritRefspec,
).build(this)

new GeneralSeedJobBuilder(
        release_code: "codereview",
        gerritBranch: gerritBranch,
        gerritRefspec: gerritRefspec,
        thisSeedJobName: seedJobName,
).build(this)
