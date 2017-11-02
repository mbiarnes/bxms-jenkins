import org.jboss.bxms.jenkins.*

//Establish the parametize release pipeline
new ReleasePipelineSeedJobBuilder(
        release_code: "bxms64",
        ci_properties_file:"/jboss-prod/config/brms-64-ci.properties",
        cfg_file:"brms-64.cfg",
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
//Generate the milestone jobs for 6.4
new JenkinsStandaloneJobBuilder(
        release_code: "bxms64",
        cfg_file:"brms-64.cfg",
        job_type: "milestone"
).build(this)

new JenkinsAllJobBuilder(
        cfg_file:"brms-64.cfg",
        release_code: "bxms64",
        job_type: "milestone"
).build(this)

new JenkinsAllJobBuilderPipeline(
        cfg_file:"brms-64.cfg",
        release_code: "bxms64",
        job_type: "milestone"
).build(this)


//Generate the milestone jobs for 6.4
new JenkinsStandaloneJobBuilder(
        cfg_file:"bxms-70la.cfg",
        release_code: "bxms70la",
        job_type: "milestone"
).build(this)

new JenkinsAllJobBuilder(
        cfg_file:"bxms-70la.cfg",
        release_code: "bxms70la",
        job_type: "milestone"
).build(this)

new JenkinsAllJobBuilderPipeline(
        cfg_file:"bxms-70la.cfg",
        release_code: "bxms70la",
        job_type: "milestone"
).build(this)

//Nightly build
new JenkinsStandaloneJobBuilder(
        cfg_file:"bxms.cfg",
        release_code: "bxms70",
        job_type: "nightly"
).build(this)

new JenkinsAllJobBuilder(
        cfg_file:"bxms.cfg",
        release_code: "bxms70",
        job_type: "nightly"
).build(this)

new JenkinsAllJobBuilderPipeline(
        cfg_file:"bxms.cfg",
        release_code: "bxms70",
        job_type: "nightly"
).build(this)