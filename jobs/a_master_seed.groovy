import org.jboss.bxms.jenkins.*

//Establish the parametize release pipeline
new ReleasePipelineSeedJobBuilder(
        release_code: "bxms64",
        ci_properties_file:"/jboss-prod/config/brms-64-ci.properties",
        cfg_file:"bxms-64.cfg",
).build(this)

new ReleasePipelineSeedJobBuilder(
        release_code: "bxms70la",
        ci_properties_file:"/jboss-prod/config/bxms-70la-ci.properties",
        cfg_file:"bxms-70la.cfg",
).build(this)

/*
new ReleasePipelineJobBuilder(
        release_code: "bxms70la",
        ci_properties_file:"/jboss-prod/config/bxms-70la-ci.properties",
        cfg_file:"bxms-70la.cfg",
        pipelineSeqFile:"release_pipeline_seq.cfg"
).build(this)
*/

new ReleasePipelineSeedJobBuilder(
        release_code: "intpack-fuse63-bxms64",
        ci_properties_file:"/jboss-prod/config/intpack-fuse63-bxms64-ci.properties",
        cfg_file:"intpack-fuse63-bxms64.cfg",
).build(this)

new ReleasePipelineSeedJobBuilder(
        release_code: "bxms-test",
        ci_properties_file:"/jboss-prod/config/bxms-test-ci.properties",
        cfg_file:"bxms-test.cfg",
).build(this)

/*
new ReleasePipelineJobBuilder(
        release_code: "bxms-test",
        ci_properties_file:"/jboss-prod/config/bxms-test-ci.properties",
        cfg_file:"bxms-test.cfg",
        pipelineSeqFile:"release_pipeline_seq.cfg"
).build(this)
*/
new GeneralSeedJobBuilder(
        release_code: "utility"
).build(this)

new GeneralSeedJobBuilder(
        release_code: "codereview"
).build(this)

//Release code is identical to the folder name in streams/
def JenkinsJobsBuilder(_release_code, _cfg_file, _job_type){
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
JenkinsJobsBuilder("bxms64", "bxms-64.cfg", "milestone" )
JenkinsJobsBuilder("bxms64", "bxms-64.cfg", "nightly" )
JenkinsJobsBuilder("bxms70la", "bxms-70la.cfg", "milestone" )
JenkinsJobsBuilder("bxms70la", "bxms-70la.cfg", "nightly" )
JenkinsJobsBuilder("intpack-fuse63-bxms64", "intpack-fuse63-bxms64.cfg", "milestone" )
JenkinsJobsBuilder("intpack-fuse63-bxms64", "intpack-fuse63-bxms64.cfg", "nightly" )
