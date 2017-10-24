import org.jboss.bxms.jenkins.*

new JenkinsStandaloneJobBuilder(
        job_name: "bxms70",
        release_code: "brms",
        job_type: "nightly"
).build(this)

new JenkinsAllJobBuilderPipeline(
        job_name: "bxms70",
        release_code: "brms",
        job_type: "nightly"
).build(this)
