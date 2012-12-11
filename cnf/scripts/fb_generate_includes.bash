#!/bin/bash

set -e
set -u

script="${0}"
scriptDir="$(dirname "${script}")"

workspaceDir="${scriptDir}/../.."
includeFile="${workspaceDir}/cnf/findbugs.include.xml"

regex='^[[:space:]]*package[[:space:]]+(.+);'

declare -a packages=( $( \
  find "${workspaceDir}" -type f -name "*.java" \
    -exec grep -E "${regex}" '{}' \; | \
    sed -r "s/${regex}/\1/g" | \
    sort -u \
  ) )


echo "<FindBugsFilter>" > "${includeFile}"

for package in "${packages[@]}"; do
 if [[ "${package}" == "org.apache.felix.bundlerepository.impl" ]]; then
   cat >> "${includeFile}" << EOF
     <Match>
       <Class name="${package}.ResolverImpl" />
     </Match>
EOF
 else
   cat >> "${includeFile}" << EOF
     <Match>
       <Package name="${package}" />
     </Match>
EOF
 fi
done

echo "</FindBugsFilter>" >> "${includeFile}"

