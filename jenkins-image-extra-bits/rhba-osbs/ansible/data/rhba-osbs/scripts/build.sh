#!/bin/bash

set -e

DEBUG=
GIT_USER=${GIT_USER:-"Your Name"}
GIT_EMAIL=${GIT_EMAIL:-"yourname@email.com"}
KERBEROS_PRINCIPAL=${KERBEROS_PRINCIPAL:-"rhpam-build/rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com"}
WORK_DIR=$(pwd)/build-temp

function help()
{
    echo "usage: build.sh [options]"
    echo
    echo "Run a cekit osbs build of an rhpam image component based on a nightly build of the upstream kie repos"
    echo
    echo "For each of the options below, the names of the arguments are environment variables that may be set"
    echo "instead of using the particular option on the invocation"
    echo ""
    echo "Required:"
    echo "  -v PROD_VERSION           Version being built. Passed to the build-overrides.sh -v option"
    echo "  -c PROD_COMPONENT         Component for which an image is being built. Valid choices are:"
    echo "                            rhpam-businesscentral, rhpam-businesscentral-monitoring, rhpam-businesscentral-indexing,"
    echo "                            rhpam-controller, rhpam-kieserver, rhpam-smartrouter, rhdm-decisioncentral,"
    echo "                            rhdm-decisioncentral-indexing, rhdm-controller, rhdm-kieserver, rhdm-optaweb-employee-rostering"
    echo "  -t OSBS_BUILD_TARGET      Build target for osbs, for example rhba-7.3-openshift-containers-candidate"
    echo ""
    echo "One of the following:"
    echo "  -s KERBEROS_PASSWORD      Password for KERBEROS_PRINCIPAL (a keytab file may be used instead via KERBEROS_KEYTAB)"
    echo "  -k KERBEROS_KEYTAB        Path to a keytab file for KERBEROS_PRINCIPAL if no KERBEROS_PASSWORD is specified."
    echo ""
    echo "Optional:"
    echo "  -h                        Print this help message"
    echo "  -p KERBEROS_PRINCIPAL     Kerberos principal to use with kinit to access build systems. Default is"
    echo "                            rhpam-build/rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com@REDHAT.COM"
    echo "  -i OSBS_BUILD_USER        Maps to the build-osbs-user option for cekit (ie the user for rhpkg commands)"
    echo "                            The default will be KERBEROS_PRINCIPAL if this is not set"
    echo "  -b BUILD_DATE             The date of the nightly build to access. Passed to the build-overrides.sh -b option if set"
    echo "  -r IMAGE_REPO             Upstream repository containing the image descriptor files for images."
    echo "                            Default is https://github.com/jboss-container-images/[rhpam|rhdm]-7-openshift-image"
    echo "  -n IMAGE_BRANCH           Branch in the IMAGE_REPO. Default is determined based on PROD_VERSION if possible"
    echo "                            (eg 7.3.0 maps to branch 7.3.x) otherwise default is 'master'"
    echo "  -d IMAGE_SUBDIR           Subdirectory in the upstream repository to descend into for the build. Default is"
    echo "                            based on the value of PROD_COMPONENT, this option is provided in case an override is needed"
    echo "  -w WORK_DIR               The working directory used for generating overrides, cekit cache, etc. Default is ./build-temp."
    echo "  -u GIT_USER               User config for git commits to internal repositories. Default is 'Your Name'"
    echo "  -e GIT_EMAIL              Email config for git commits to internal repositories. Default is 'yourname@email.com'"
    echo "  -o CEKIT_BUILD_OPTIONS    Additional options to pass through to the cekit build command, should be quoted"
    echo "  -l CEKIT_CACHE_LOCAL      Comma-separated list of urls to download and add to the local cekit cache"
    echo "  -g                        Debug setting, currently sets verbose flag on cekit commands"
}


function get_short_version() {
  local version_array
  local short_version=$1
  IFS='.' read -r -a version_array <<< "$1"
  if [ ${#version_array[@]} -gt 1 ]; then
      short_version="${version_array[0]}.${version_array[1]}"
  fi
  echo $short_version
}

function check_for_required_envs()
{
    if [ -z "$KERBEROS_PRINCIPAL" ]; then
        echo "No kerberos principal specified with KERBEROS_PRINCIPAL"
        exit -1
    fi
    if [ -z "$GIT_EMAIL" ]; then
        echo "No git email specified with GIT_EMAIL"
        exit -1
    fi
    if [ -z "$GIT_USER" ]; then
        echo "No git user specified with GIT_USER"
        exit -1
    fi
    if [ -z "$PROD_VERSION" ]; then
        echo "No version specified with PROD_VERSION"
        exit -1
    fi
    if [ -z "$PROD_COMPONENT" ]; then
        echo "No component specified with PROD_COMPONENT"
        exit -1
    else
        case "$PROD_COMPONENT" in
            rhpam-businesscentral | \
            rhpam-businesscentral-indexing | \
            rhpam-businesscentral-monitoring | \
            rhpam-controller | \
            rhpam-kieserver | \
            rhpam-smartrouter)
                if [ -z "$IMAGE_SUBDIR" ]; then
                    IMAGE_SUBDIR=${PROD_COMPONENT#"rhpam-"}
                    echo Default component subdir is "$IMAGE_SUBDIR"
                fi		
		;;
	    rhdm-controller | \
	    rhdm-decisioncentral | \
	    rhdm-decisioncentral-indexing | \
	    rhdm-kieserver | \
	    rhdm-optaweb-employee-rostering)
                if [ -z "$IMAGE_SUBDIR" ]; then
                    IMAGE_SUBDIR=${PROD_COMPONENT#"rhdm-"}
                    echo Default component subdir is "$IMAGE_SUBDIR"
                fi
                ;;
            *)
                echo Invalid subcomponent specified with PROD_COMPONENT
                exit -1
                ;;
        esac
    fi
    if [ -z "$OSBS_BUILD_TARGET" ]; then
        echo "No build target specified with OSBS_BUILD_TARGET"
        exit -1
    fi
}

function get_default_image_repo() {
    if [ -n "$IMAGE_REPO" ]; then
	return
    fi
    case "$PROD_COMPONENT" in
        rhpam-businesscentral | \
        rhpam-businesscentral-indexing | \
        rhpam-businesscentral-monitoring | \
        rhpam-controller | \
        rhpam-kieserver | \
        rhpam-smartrouter)
	    IMAGE_REPO="https://github.com/jboss-container-images/rhpam-7-openshift-image"
            ;;
        rhdm-controller | \
        rhdm-decisioncentral | \
        rhdm-decisioncentral-indexing | \
	rhdm-kieserver | \
	rhdm-optaweb-employee-rostering)
	    IMAGE_REPO="https://github.com/jboss-container-images/rhdm-7-openshift-image"
	    ;;
	*)
	    ;;
    esac
}

function get_kerb_ticket() {
    set +e
    if [ -n "$KERBEROS_PASSWORD" ]; then
        echo "$KERBEROS_PASSWORD" | kinit "$KERBEROS_PRINCIPAL"
        if [ "$?" -ne 0 ]; then
            echo "Failed to get kerberos token for $KERBEROS_PRINCIPAL with password"
            exit -1
        fi
    elif [ -n "$KERBEROS_KEYTAB" ]; then
        kinit -k -t "$KERBEROS_KEYTAB" "$KERBEROS_PRINCIPAL"
        if [ "$?" -ne 0 ]; then
            echo "Failed to get kerberos token for $KERBEROS_PRINCIPAL with $KERBEROS_KEYTAB"
            exit -1
        fi
    else
        echo "No kerberos password or keytab specified with KERBEROS_PASSWORD or KERBEROS_KEYTAB"
        exit -1
    fi
    set -e
}

function set_git_config()
{
    git config --global user.email "$GIT_EMAIL"
    git config --global user.name  "$GIT_USER"
    git config --global core.pager ""
}

function find_image_branch()
{
    if [ -n "$IMAGE_BRANCH" ]; then
	echo Using image branch "$IMAGE_BRANCH"
	return
    fi

    # Try to figure out the image branch from the prod version
    local sv=$(get_short_version "$PROD_VERSION")
    if [ "$sv" == "master" ]; then
	IMAGE_BRANCH=master
    else
	# if we can find "$sv.x" as a remote, use it
        set +e
	git branch -a | grep remotes/origin/"$sv".x
        local res=$?
        set -e
	if [ "$res" -eq 0 ]; then
	    IMAGE_BRANCH="$sv.x"
	else
	    IMAGE_BRANCH=master
	fi
    fi
    echo Using image branch "$IMAGE_BRANCH"
}

function download_build_overrides()
{
    rm -rf jboss-kie-modules
    git clone https://github.com/jboss-container-images/jboss-kie-modules
    if [ "$?" -ne 0 ]; then
        echo Unable to clone repository https://github.com/jboss-container-images/jboss-kie-modules
        exit -1
    fi
    pushd jboss-kie-modules
    set +e
    if [ "$IMAGE_BRANCH" == "master" ]; then
        git checkout master
    else
        git checkout --track origin/"$IMAGE_BRANCH"
    fi
    local res=$?
    set -e
    if [ "$res" -ne 0 ]; then
        echo Command failed: git checkout --track origin/"$IMAGE_BRANCH"
        echo Unable to checkout designated branch "$IMAGE_BRANCH"
        echo This probably means that "$IMAGE_BRANCH" is not an actual remote branch in "$IMAGE_REPO"
        exit -1
    fi
    mkdir -p $WORK_DIR/tools
    cp tools/build-overrides/build-overrides.sh $WORK_DIR/tools/
    popd

}

function clone_repo_and_set_dir()
{
    set +e
    rm -rf rhpam-repo
    # Try to check the repo without git asking for credentials (which it does for missing repos with https urls)
    wget "$IMAGE_REPO" --no-check-certificate -o /dev/null
    if [ "$?" -ne 0 ]; then
        echo It does not look like "$IMAGE_REPO" specified with IMAGE_REPO is a valid url
        exit -1
    fi
    echo git clone "$IMAGE_REPO" rhpam-repo
    git clone "$IMAGE_REPO" rhpam-repo
    local res=$?
    if [ "$res" -ne 0 ]; then
        echo Unable to clone repository "$IMAGE_REPO"
        exit -1
    fi
    set -e
    cd rhpam-repo
    
    find_image_branch
    set +e
    if [ "$IMAGE_BRANCH" == "master" ]; then
	git checkout master
    else
	git checkout --track origin/"$IMAGE_BRANCH"
    fi
    local res=$?
    set -e
    if [ "$res" -ne 0 ]; then
	echo Command failed: git checkout --track origin/"$IMAGE_BRANCH"
	echo Unable to checkout designated branch "$IMAGE_BRANCH"
	echo This probably means that "$IMAGE_BRANCH" is not an actual remote branch in "$IMAGE_REPO"
	exit -1
    fi
    cd "$IMAGE_SUBDIR"
}

function handle_overrides()
{
    overrides=
    extraoverrides=

    # We don't have any overrides for indexing images, and build-overrides.sh doesn't support them as a product value
    if [ "$PROD_COMPONENT" == "rhdm-decisioncentral-indexing" -o "$PROD_COMPONENT" == "rhpam-businesscentral-indexing" ]; then
	return
    fi

    # Get the build-overrides.sh script itself, based on the branch we're using
    download_build_overrides

    overrides="--overrides-file $WORK_DIR/build-overrides/$PROD_COMPONENT-overrides.yaml"

    # If we have an additional overrides file included for a particular branch and a particular
    # component that we're building, then add that extra file
    local sv=$(get_short_version $PROD_VERSION)
    if [ -f "/opt/rhpam/overrides/$sv/$PROD_COMPONENT-overrides.yaml" ]; then
	extraoverrides="--overrides-file /opt/rhpam/overrides/$sv/$PROD_COMPONENT-overrides.yaml"
    fi

    # Include a build date arg if it's set, otherwise leave it null
    local buildopt=
    if [ -n "$BUILD_DATE" ]; then
	buildopt="-b $BUILD_DATE"
    fi

    # Generate the overrides files
    echo $WORK_DIR/tools/build-overrides.sh -v $PROD_VERSION -t nightly -p $PROD_COMPONENT $buildopt -d $WORK_DIR/build-overrides $bo_workdir

    $WORK_DIR/tools/build-overrides.sh -v $PROD_VERSION -t nightly -p $PROD_COMPONENT $buildopt -d $WORK_DIR/build-overrides $bo_workdir

# If we think it's ever the case that skipping the overrides generation in case of error is okay, we can do this
#    set +e
#    $WORK_DIR/tools/build-overrides.sh -v $PROD_VERSION -t nightly -p $PROD_COMPONENT $buildopt -d $WORK_DIR/build-overrides $bo_workdir
#    res=$?
#    set -e
#    if [ "$res" -ne 0 ]; then
#        overrides=
#    fi
    
}

function handle_cache_urls()
{
    # Include a build date arg if it's set, otherwise leave it null
    local buildopt
    if [ -n "$BUILD_DATE" ]; then
	buildopt="-b $BUILD_DATE"
    fi

    # See if there is a file for this version/component stored with the overrides that specifies urls to cache
    local sv=$(get_short_version $PROD_VERSION)
    if [ -f "/opt/rhpam/overrides/$sv/$PROD_COMPONENT-cache-local.sh" ]; then
        echo $WORK_DIR/tools/build-overrides.sh $buildopt -C /opt/rhpam/overrides/$sv/$PROD_COMPONENT-cache-local.sh $bo_workdir
        $WORK_DIR/tools/build-overrides.sh $buildopt -C /opt/rhpam/overrides/$sv/$PROD_COMPONENT-cache-local.sh $bo_workdir
    fi

    # Parse and cache extra urls to add to the local cekit cache
    if [ -n "$1" ]; then
        local IFS=,
        local urllist=($1)
        for url in "${urllist[@]}"; do
            echo $WORK_DIR/tools/build-overrides.sh $buildopt -c $url $bo_workdir
            $WORK_DIR/tools/build-overrides.sh $buildopt -c $url $bo_workdir
        done
    fi
}

while getopts gu:e:v:c:t:o:r:n:d:p:k:s:b:l:i:w:h option; do
    case $option in
        g)
            DEBUG=true
            ;;
        u)
            GIT_USER=$OPTARG
            ;;
        e)
            GIT_EMAIL=$OPTARG
            ;;
        v)
            PROD_VERSION=$OPTARG
            ;;
        c)
            PROD_COMPONENT=$OPTARG
            ;;
        t)
            OSBS_BUILD_TARGET=$OPTARG
            ;;
        o)
            CEKIT_BUILD_OPTIONS=$OPTARG
            ;;
        r)
            IMAGE_REPO=$OPTARG
            ;;
        n)
            IMAGE_BRANCH=$OPTARG
            ;;
        d)
            IMAGE_SUBDIR=$OPTARG
            ;;
        p)
            KERBEROS_PRINCIPAL=$OPTARG
            ;;
        k)
            KERBEROS_KEYTAB=$OPTARG
            ;;
        s)
            KERBEROS_PASSWORD=$OPTARG
            ;;
        b)
            BUILD_DATE=$OPTARG
            ;;
	l)
	    CEKIT_CACHE_LOCAL=$OPTARG
	    ;;
	i)
	    OSBS_BUILD_USER=$OPTARG
	    ;;
	w)
	    WORK_DIR=$OPTARG
	    ;;
        h)
            help
            exit 0
            ;;
        *)
            ;;
    esac
done
shift $((OPTIND-1))

mkdir -p $WORK_DIR
cd $WORK_DIR

# Set the working directory for cekit and cekit-cache commands and also an option to pass to build-overrides.sh
cekit_workdir="--work-dir=$WORK_DIR/.cekit"
bo_workdir="-w $WORK_DIR/.cekit"

check_for_required_envs
get_default_image_repo
clone_repo_and_set_dir
get_kerb_ticket
set_git_config
handle_overrides
handle_cache_urls "$CEKIT_CACHE_LOCAL"

debug=
if [ -n "$DEBUG" ]; then
    debug="-v"
fi

builduser=
if [ -n "$OSBS_BUILD_USER" ]; then
    builduser="--build-osbs-user=$OSBS_BUILD_USER"
fi

# Invoke cekit and respond with Y to any prompts
echo cekit $debug --config /opt/rhpam/cekit/config --overrides-file branch-overrides.yaml $overrides $extraoverrides --build-engine=osbs --build-osbs-target=$OSBS_BUILD_TARGET $builduser $cekit_workdir $CEKIT_BUILD_OPTIONS build


yes Y | cekit $debug --config /opt/rhpam/cekit/config --overrides-file branch-overrides.yaml $overrides $extraoverrides --build-engine=osbs --build-osbs-target=$OSBS_BUILD_TARGET $builduser $cekit_workdir $CEKIT_BUILD_OPTIONS build
