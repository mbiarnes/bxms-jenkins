import org.jboss.bxms.jenkins.*

//Establish the parametize release pipeline
//Release code is identical to the folder name in streams/

def ReleasePipelineBuilder(_release_code, _cfg_file, _properties_file) {
    new ReleasePipelineSeedJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            ci_properties_file:_properties_file
    ).build(this)

    new ReleasePipelineJobBuilder(
            release_code: _release_code,
            cfg_file:_cfg_file,
            ci_properties_file:_properties_file,
    ).build(this)
}
ReleasePipelineBuilder("bxms", "bxms.cfg", "/jboss-prod/config/bxms-ci.properties" )
ReleasePipelineBuilder("bxms-test", "bxms-test.cfg", "/jboss-prod/config/bxms-test-ci.properties" )

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
JenkinsJobsBuilder("bxms", "bxms.cfg", "milestone" )
JenkinsJobsBuilder("bxms", "bxms-dev.cfg", "nightly" )

new GeneralSeedJobBuilder(
        release_code: "utility"
).build(this)

new GeneralSeedJobBuilder(
        release_code: "codereview"
).build(this)
