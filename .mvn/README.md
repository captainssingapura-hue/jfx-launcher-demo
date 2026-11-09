This folder marks the repository root for Maven: with it, `maven.multiModuleProjectDirectory`
is this directory whichever pom a build starts from. `sample-estate` uses it to deploy its
jars to `dist/acme-repo`, where the estate launcher looks for them.
