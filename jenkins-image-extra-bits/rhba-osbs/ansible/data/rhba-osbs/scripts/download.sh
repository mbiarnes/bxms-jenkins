#!/bin/bash

set -e

WORK_DIR=$(pwd)/build-temp

function help()
{
    echo "usage: download.sh [options]"
    echo
    echo "Utility script to download resources for an rhba image build."
    echo ""
    echo "This script is meant to be used in a Jenkins job to stage an image build for rhba."
    echo "It checks out the correct branch from jboss-kie-modules and jboss-container-images repos and"
    echo "echos the path for the component being built."
    echo ""
    echo ""
    echo "For each of the options below, the names of the arguments are environment variables that may be set"
    echo "instead of using the particular option on the invocation"
    echo ""
    echo "Required:"
    echo "  -v PROD_VERSION           Version being built"
    echo "  -c PROD_COMPONENT         Component for which an image is being built. Valid choices are:"
    echo "                            rhpam-businesscentral, rhpam-businesscentral-monitoring, rhpam-businesscentral-indexing,"
    echo "                            rhpam-controller, rhpam-kieserver, rhpam-smartrouter, rhdm-decisioncentral,"
    echo "                            rhdm-decisioncentral-indexing, rhdm-controller, rhdm-kieserver, rhdm-optaweb-employee-rostering"
    echo ""
    echo "Optional:"
    echo "  -h                        Print this help message"
    echo "  -r IMAGE_REPO             Upstream repository containing the image descriptor files for images."
    echo "                            Default is https://github.com/jboss-container-images/[rhpam|rhdm]-7-openshift-image"
    echo "  -n IMAGE_BRANCH           Branch in the IMAGE_REPO. Default is determined based on PROD_VERSION if possible"
    echo "                            (eg 7.3.0 maps to branch 7.3.x) otherwise default is 'master'"
    echo "  -d IMAGE_SUBDIR           Subdirectory in the upstream repository to descend into for the build. Default is"
    echo "                            based on the value of PROD_COMPONENT, this option is provided in case an override is needed"
    echo "  -w WORK_DIR               The working directory used to hold the build resources. Default is ./build-temp."
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

function get_build_overrides_script()
{
    pushd $WORK_DIR > /dev/null
    rm -rf jboss-kie-modules
    git clone https://github.com/jboss-container-images/jboss-kie-modules
    if [ "$?" -ne 0 ]; then
        echo Unable to clone repository https://github.com/jboss-container-images/jboss-kie-modules
        exit -1
    fi
    cd jboss-kie-modules
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
    echo Copying build-overrides.sh to $WORK_DIR
    cp tools/build-overrides/build-overrides.sh $WORK_DIR
    echo Copying build-osbs.sh to $WORK_DIR
    cp tools/build-osbs/build-osbs.sh $WORK_DIR
    popd > /dev/null
}

function clone_repo_and_set_dir()
{
    set +e
    rm -rf rhba-repo
    # Try to check the repo without git asking for credentials (which it does for missing repos with https urls)
    wget "$IMAGE_REPO" --no-check-certificate -o /dev/null -O /dev/null
    if [ "$?" -ne 0 ]; then
        echo It does not look like "$IMAGE_REPO" specified with IMAGE_REPO is a valid url
        exit -1
    fi
    echo git clone "$IMAGE_REPO" rhba-repo
    git clone "$IMAGE_REPO" rhba-repo
    local res=$?
    if [ "$res" -ne 0 ]; then
        echo Unable to clone repository "$IMAGE_REPO"
        exit -1
    fi
    set -e
    cd rhba-repo

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
    cd ./"$IMAGE_SUBDIR"
}

while getopts gu:e:v:c:t:o:r:n:d:p:k:s:b:l:i:w:h option; do
    case $option in
        g)
            DEBUG=true
            ;;
        v)
            PROD_VERSION=$OPTARG
            ;;
        c)
            PROD_COMPONENT=$OPTARG
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

check_for_required_envs
get_default_image_repo

# After this call, we'll be in the correct branch and subdirectory for the image being built
clone_repo_and_set_dir

# Download the build-overrides.sh script based on the branch we're building
# Note get_build_overrides_script must follow clone_repo_and_set_dir
# Branches across the image repo and the jboss-kie-modules repo are parallel
# this routine uses pushd/popd to preserve the directory
get_build_overrides_script

# This is very important, we want the current working directory printed as the last line
pwd
