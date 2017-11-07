package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import ca.szc.configparser.Ini

import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.io.StringReader



/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class JenkinsStandaloneJobBuilder {
    String release_code
    String job_type
    String cfg_file

    Map<String, String> maven_repo_map=["intpack-fuse63-bxms64":"/jboss-prod/m2/bxms-6.4-", "bxms64":"/jboss-prod/m2/bxms-6.4-", "bxms70la":"/jboss-prod/m2/bxms-7-", "bxms":"/jboss-prod/m2/bxms-7-", "bxms-test":"/jboss-prod/m2/bxms-7-"]
    Map<String, String> repo_group_map=["milestone":"MEAD", "nightly":"MEAD+JENKINS+JBOSS+CENTRAL"]
    Job build(DslFactory dslFactory) {
        String urlString ="https://code.engineering.redhat.com/gerrit/gitweb?p=integration-platform-config.git;a=blob_plain;f=" + cfg_file
        URL cfg_url = urlString.toURL()
        BufferedReader configReader = newReader(cfg_url.getHost(), cfg_url.getFile())
        Ini _ini_cfg = new Ini().read(configReader)
        Map<String,Map<String,String>> sections = _ini_cfg.getSections()

        dslFactory.folder(release_code + "-jenkins-" + job_type + "-pipeline")
        String maven_repo = maven_repo_map [release_code] + job_type
        String repo_group = repo_group_map [job_type]
        String _cfg = cfg_file

        for (String section_name : sections.keySet())
        {
            Map<String, String> section=sections.get(section_name)
            if ((!section.containsKey("config_type")) || (section.containsKey("config_type") && section.get("config_type").equals("bom-builder")) )
            {

                String shellScript = """
unset WORKSPACE
MVN_DEP_REPO=nexus-release::default::file://${maven_repo} REPO_GROUP=${repo_group} LOCAL=1 CFG=${_cfg} MVN_LOCAL_REPO=${maven_repo} POMMANIPEXT=bxms-bom make DEBUG=\$DEBUG ${section_name}
"""
                dslFactory.job(release_code + "-jenkins-" + job_type + "-pipeline/" + release_code + "-" + section_name ) {
                    it.description "This job is a seed job for generating " + release_code + " " + job_type + " jenkins build."
                    logRotator {
                        numToKeep 8
                    }
                    parameters {
                        // Defines a simple text parameter, where users can enter a string value.
                        booleanParam('DEBUG', false, 'Open Debug Log')
                    }

                    label("bxms-nightly")

                    multiscm {

                        // Adds a Git SCM source.
                        git {

                            // Adds a remote.
                            remote {

                                // Sets the remote URL.
                                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                            }
                            // Specify the branches to examine for changes and to build.
                            branch("master")
                        }

                        // Adds a Git SCM source.
                        git {

                            // Adds a remote.
                            remote {

                                // Sets the remote URL.
                                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling")
                            }

                            // Specify the branches to examine for changes and to build.
                            branch("master")

                            // Adds additional behaviors.
                            extensions {

                                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                                relativeTargetDirectory('ip-tooling')
                            }
                        }
                    }
                    wrappers {
                        // Deletes files from the workspace before the build starts.
                        preBuildCleanup(){
                            includePattern('workspace/**')
                            deleteDirectories()
                        }

                    }

                    steps {
                        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
                        shell(shellScript)
                    }

                }
            }

        }

    }

    BufferedReader newReader(String hostname, String file)
    {
        Socket baseSocket = new Socket()
        baseSocket.connect(new InetSocketAddress(hostname, 443))

        X509TrustManager trustAllTrustManager = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers(){return null}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }
        SSLContext sslContext = SSLContext.getInstance("TLS")
        TrustManager[] trustAllTrustManagers = [ trustAllTrustManager ]
        sslContext.init(null, trustAllTrustManagers, new SecureRandom())
        SSLSocketFactory factory = sslContext.getSocketFactory()
        // Intentionally blank. Hostname was given to baseSocket so that sslSocket can be unaware of it.
        String blankHostname = ""
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(baseSocket, blankHostname, baseSocket.getPort(), true)
        sslSocket.setUseClientMode(true)

        sslSocket.startHandshake()

        InputStream inStream = sslSocket.getInputStream()
        OutputStream outStream = sslSocket.getOutputStream()

        BufferedReader read = new BufferedReader(new InputStreamReader(inStream))
        BufferedWriter write = new BufferedWriter(new OutputStreamWriter(outStream))

        // Write request lines
        // Method and File
        write.write("GET ")
        write.write(file)
        write.write(" HTTP/1.0")
        write.write("\r\n")
        // Host
        write.write("Host: ")
        write.write(hostname)
        write.write("\r\n")
        // Close connection after reply
        write.write("Connection: close")
        write.write("\r\n")
        // Finish request
        write.write("\r\n")
        // Send to server
        write.flush()

        // Read reply
        String line

        // Look at status line
        line = read.readLine()
        if (!"HTTP/1.1 200 OK".equals(line))
            throw new IOException("Bad HTTP server status: " + line)

        // Read through the HTTP header
        while ((line = read.readLine()) != null)
            if ("".equals(line))
                break

        // Return the reader for caller to consume from
        return read
    }

}
