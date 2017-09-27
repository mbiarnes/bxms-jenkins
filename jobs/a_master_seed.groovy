import org.jboss.bxms.jenkins.*

//Establish the parametize release pipeline
new ReleasePipelineSeedJobBuilder(
        release_code: "brms-64",
        ci_properties_file:"/jboss-prod/config/brms-64-ci.properties",
        cfg_file:"brms-64.cfg",
).build(this)

new ReleasePipelineSeedJobBuilder(
        release_code: "brms",
        ci_properties_file:"/jboss-prod/config/brms-ci.properties",
        cfg_file:"brms.cfg",
).build(this)

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

new GeneralSeedJobBuilder(
        stream_name: "utility"
).build(this)

new GeneralSeedJobBuilder(
        stream_name: "codereview"
).build(this)

//Release code is the prefix for cfg file
new JenkinsStandaloneJobBuilder(
        job_name: "bxms64",
        release_code: "brms-64",
        job_type: "milestone"
).build(this)

new JenkinsAllJobBuilder(
        job_name: "bxms64",
        release_code: "brms-64",
        job_type: "milestone"
).build(this)


new JenkinsStandaloneJobBuilder(
        job_name: "bxms70",
        release_code: "brms",
        job_type: "milestone"
).build(this)

new JenkinsAllJobBuilder(
        job_name: "bxms70",
        release_code: "brms",
        job_type: "milestone"
).build(this)

new JenkinsAllJobBuilderPipeline(
        job_name: "bxms70",
        release_code: "brms",
        job_type: "milestone"
).build(this)

new JenkinsStandaloneJobBuilder(
        job_name: "bxms70",
        release_code: "bxms",
        job_type: "nightly"
).build(this)

new JenkinsAllJobBuilder(
        job_name: "bxms70",
        release_code: "bxms",
        job_type: "nightly"
).build(this)

new JenkinsAllJobBuilderPipeline(
        job_name: "bxms70",
        release_code: "bxms",
        job_type: "nightly"
).build(this)
