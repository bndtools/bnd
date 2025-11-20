# Sonatype Deployment

## configuration types

1. release, snapshot

    1. snapshot deployments

        `build.bnd` - `#-snapshot:`

    2. release deployments
        
        RC:         `build.bnd` - `-snapshot: RC1`
        RELEASE:    `build.bnd` -> `-snapshot:`

2. release, staging, snapshot

## execution types

cli, maven, gradle, Eclipse UI

## entities

* multiple bundles
    * release and not released
* sub-bundles
* exported jar

# sonatype lifecycle

1. upload & manual deployment
2. upload & auto deployment