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
ReleasePipelineBuilder("bxms64", "bxms-64.cfg", "/jboss-prod/config/bxms-64-ci.properties" )
ReleasePipelineBuilder("bxms70la", "bxms-70la.cfg", "/jboss-prod/config/bxms-70la-ci.properties" )
ReleasePipelineBuilder("intpack-fuse63-bxms64", "intpack-fuse63-bxms64.cfg", "/jboss-prod/config/intpack-fuse63-bxms64-ci.properties" )

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

new GeneralSeedJobBuilder(
        release_code: "utility"
).build(this)

new GeneralSeedJobBuilder(
        release_code: "codereview"
).build(this)
