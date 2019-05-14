The rhba-osbs role (and potentially others in the future) require a
kerberos service principal that can be used from a Jenkins slave
image. The kerberos principal that has been created for this task is:

    rhba-build/rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com

We do not have a password for this user, but we do have a
keytab file which is located in this directory:

    rhba-build.keytab
    rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com.keytab

The keytab is used with kinit like this to get a kerberos ticket:

```bash
kinit -k -t rhba-build.keytab rhba-build/rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com
```

This file should be protected like a password.
It is also stored as a secretfile in the rhba Jenkins instance.
