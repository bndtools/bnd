#!/bin/bash

set -e
set -u

script="${0}"
scriptDir="$(dirname "${script}")"

workspaceDir="${scriptDir}/../.."
excludeFile="${workspaceDir}/cnf/findbugs.include.xml"

regex='^[[:space:]]*package[[:space:]]+(.+);'

declare -a packages=( $( \
  find "${workspaceDir}" -type f -name "*.java" \
    -exec grep -E "${regex}" '{}' \; | \
    sed -r "s/${regex}/\1/g" | \
    sort -u \
  ) )


echo "<FindBugsFilter>" > "${excludeFile}"

for package in "${packages[@]}"; do
  cat >> "${excludeFile}" << EOF
     <Match>
       <Package name="${package}" />
     </Match>
EOF
done

echo "</FindBugsFilter>" >> "${excludeFile}"

