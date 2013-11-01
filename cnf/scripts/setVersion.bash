#!/bin/bash

set -e
set -u


# these are the files in which the version must be adjusted
declare -a filesToAdjust=( \
  build/feature/extras/ace/feature.xml \
  build/feature/extras/amdatu/feature.xml \
  build/feature/extras/dm/feature.xml \
  build/feature/extras/category.xml \
  build/feature/main/bndtools/feature.xml \
  build/feature/main/category.xml \
  build/feature/main/jarviewer/feature.xml \
  cnf/build.bnd \
)

declare -a filesToAdjustNoQualifier=( \
  bndtools.core/resources/intro/whatsnewExtensionContent.xml \
)


script="${0}"
scriptDir="$(dirname "${script}")"
cnfDir="${scriptDir}/.."
workspaceDir="${cnfDir}/.."


#
# Trim a string: remove spaces from the beginning and end of the string
#
# 1=string to trim  return=trimmed string
function stringTrim() {
  if [[ -z "${1}" ]]; then
    return
  fi

  # remove leading whitespace characters
  local var="${1#${1%%[![:space:]]*}}"

  # remove trailing whitespace characters
  echo "${var%${var##*[![:space:]]}}"
}


#
# Get the current version from the cnf/build.bnd file
#
function getCurrentVersion() {
  local regex='[[:space:]]*base-version[[:space:]]*:[[:space:]]*(.+)[[:space:]]*'
  grep -E "^${regex}\$" "${cnfDir}/build.bnd" | sed -r "s/${regex}/\1/"
}


#
# Validate the new version
#
function validateNewVersion() {
  local newVersion="$(stringTrim "${1}")"

  local regex="[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+\.(DEV|RC[[:digit:]]+|REL)"
  local validated="$(echo "${newVersion}" | grep -E "^${regex}$")"

  if [[ -z "${validated}" ]]; then
    echo "ERROR: Version ${newVersion} is not a proper version."
    echo "       The regular expression for a proper version looks is"
    echo "         ${regex}"
    exit 1
  fi
}


function stripQualifier() {
  echo "${1}" | sed 's/\.[^\.]*$//'
}


function checkCleanWorkspace() {
  pushd "${workspaceDir}" &> /dev/null
  local status="$(git describe --always --dirty=-DIRTY)"
  if [ -n "$(echo "${status}" | grep -E '\-DIRTY$')" ]; then
    echo "ERROR: the workspace is dirty, first clean it up by doing"
    echo "         git clean -fdx"
    echo "         git reset --hard"
    exit 1
  fi

  popd &> /dev/null
}


#
# Adjust the versions
#
function adjustVersion() {
  local currentVersion="$(stringTrim "${1}")"
  local newVersion="$(stringTrim "${2}")"
  local currentVersionNoQualifier="$(stripQualifier "${currentVersion}")"
  local newVersionNoQualifier="$(stripQualifier "${newVersion}")"

  local fileToAdjust=""
  if [ ${#filesToAdjust[*]} -gt 0 ]; then
    for fileToAdjust in "${filesToAdjust[@]}"; do
      sed -i "s/${currentVersion}/${newVersion}/g" "${fileToAdjust}"
      git add "${fileToAdjust}"
    done
  fi

  if [ ${#filesToAdjustNoQualifier[*]} -gt 0 ]; then
    for fileToAdjust in "${filesToAdjustNoQualifier[@]}"; do
      sed -i "s/${currentVersionNoQualifier}/${newVersionNoQualifier}/g" "${fileToAdjust}"
      git add "${fileToAdjust}"
    done
  fi
}




#
# Main
#

if [[ ${#} -ne 1 ]]; then
  echo "ERROR: Specify the new version"
  echo "       Example versions:"
  echo "         5.3.8.DEV"
  echo "         5.3.8.RC1"
  echo "         5.3.8.REL"
  exit 1
fi

checkCleanWorkspace

declare newVersion="$(stringTrim "${1}")"
validateNewVersion "${newVersion}"

declare currentVersion="$(getCurrentVersion)"
if [[ -z "${currentVersion}" ]]; then
  echo "ERROR: Could not determine current version"
  exit 1
fi

adjustVersion "${currentVersion}" "${newVersion}"
git commit -s -m "Update to version ${newVersion}"
