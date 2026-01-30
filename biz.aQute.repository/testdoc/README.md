# Test documentation

here you find bnd repository configurations to analyse Sonatype release/snapshot deployments
copy following files into your `/cnf/ext` folder and refresh to activate repos

MIND additional requirements/restrictions on SNAPSHOTS

* configuration of the Sonatype namespace bearer token is required
* `version_biz.aQute.bnd` inside `DBG_sonatype.bnd`

`DBG_sonatype.bnd` - configures the repo and snapshot version
`DBG_sonatype.mvn` - explicit list of bundles
