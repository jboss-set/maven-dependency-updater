# Maven Dependency Updater

This project provides a CLI tool and a maven plugin that report possible dependency upgrades in Maven projects.
The dependency upgrades can also be directly performed in the POM file.

To limit which component upgrades should be reported, configuration file with per-dependency upgrade rules can be
created. For example, it can be defined that we want certain dependency to be upgraded only to versions with particular
prefix, and some other dependency only to versions with particular qualifier. The default behaviour is that only 
upgrades to newer micro versions are reported. 

It can be also specified which maven repositories should be used to look up new dependency versions. By default
the Maven Central repository is used.

- [Usage - Maven plugin](#usage---maven-plugin)
- [Usage - CLI](#usage---cli)
  - [CLI Usage examples](#cli-usage-examples)
- [Configuration](#configuration)
  - [Configuration Keys](#configuration-keys)
  - [Upgrade Rules Examples](#upgrade-rules-examples)
- [Limitations](#limitations)


## Usage - Maven plugin

Prerequisite: add the JBoss Releases repository as a plugin repository in your Maven project:

```xml
  <pluginRepositories>
    <pluginRepository>
      <id>jboss-releases-repository</id>
      <name>JBoss Releases Repository</name>
      <url>https://repository.jboss.org/nexus/content/repositories/releases/</url>
    </pluginRepository>
  </pluginRepositories>
```

Or instead you can add the plugin repository in your `${user.home}/.m2/settings.xml`:

```xml
<settings>
  ...
  <profiles>
    ...
    <profile>
      <id>jboss-releases-repo-profile</id>
      <pluginRepositories>
        <pluginRepository>
          <id>jboss-releases-repo</id>
          <name>JBoss Releases Repository</name>
          <url>https://repository.jboss.org/nexus/content/repositories/releases/</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
    ...
  </profiles>

  <activeProfiles>
    <activeProfile>jboss-releases-repo-profile</activeProfile>
  </activeProfiles>
</settings>
```

To generate reports:

```bash
mvn org.jboss.set.dependency-alignment:dependency-updater-maven-plugin:report
```

To perform dependency upgrades:

```bash
mvn org.jboss.set.dependency-alignment:dependency-updater-maven-plugin:perform-upgrades
```

In order to make the maven commands shorter, add the group "org.jboss.set.dependency-alignment" as a plugin group in your
`${user.home}/.m2/settings.xml`: 

```xml
  <pluginGroups>
    <pluginGroup>org.jboss.set.dependency-alignment</pluginGroup>
  </pluginGroups>
```

After that, you can use plugin prefix:

```bash
mvn dependency-updater:report
mvn dependency-updater:perform-upgrades
```

Reports will be saved in `dependency-upgrades-report.txt` files in `target/` directories of maven modules. If no
upgradeable dependencies are found, the report file for given module will not be generated.

If you want to define upgrade rules, place `dependency-upgrade-config.json` file into your root project directory.
See [Configuration](#configuration) section for more information about the configuration file options. 

## Usage - CLI

```bash
java -jar <path/to/alignment-cli.jar> <action> -f <path/to/pom.xml> [-c <path/to/configuration.json>] [-o output-file.txt]
```

- alignment-cli-\<version\>.jar file is generated during build in `$SOURCE_DIR/cli/target/`,
- `<action>` can be "generate-report" or "perform-upgrades".

### CLI Usage Examples

Generate text report with possible dependency upgrades to an output file `report.txt`:

```bash
$ java -jar cli/target/alignment-cli-0.3.jar generate-report -c path/to/configuration.json -f path/to/pom.xml -o report.txt
```

Perform possible dependency upgrades in the POM:

```bash
$ java -jar $CLI_JAR perform-upgrades -c path/to/configuration.json -f path/to/pom.xml
```

## Configuration

An optional configuration file containing upgrade rules can be prepared.

Example `configuration.json`:

```json
{
  "repositories" : {
    "Central": "https://repo1.maven.org/maven2/",
    "JBossPublic": "https://repository.jboss.org/nexus/content/repositories/public/"
  },
  "ignoreScopes": ["test"],
  "rules": {
    "*:*": {
      "STREAM": "MICRO"
    },
    "commons-cli:commons-cli": {
      "STREAM": "MINOR"
    },
    "org.picketlink:*": {
      "PREFIX": "2.5.5",
      "QUALIFIER": "SP\\d+"
    }
  }
}
```

### Configuration keys

* `repositories`: A map of repositories where new dependency versions will be looked up.
* `ignoreScopes`: A list of maven dependency scopes to be ignored.
* `rules`: A map where keys are of the format "groupId:artifactId" and values are _upgrade rules_.

  `groupId` and `artifactId` can be a wildcard "*".

  _Upgrade rule_ is either:
  
  * a string "NEVER", which means never to upgrade given G:A.
  * a map which can contain with following keys:
    * `PREFIX`: version prefix, e.g. "1.2.3", that candidate versions has to match.
    * `QUALIFIER`: a single regular expression pattern or a list of patterns, one of which must match a candidate versions' qualifier.
    * `STREAM`: a stream name.
    
  _Stream names_: 
    * `MAJOR` - upgrade to the latest MAJOR version (i.e. no restrictions).
    * `MINOR` - upgrade to the latest MINOR version, MAJOR must not change.
    * `MICRO` - upgrade to the latest MICRO version, MAJOR and MINOR must not change.
    * `QUALIFIER` - upgrade to the latest QUALIFIER version, MAJOR, MINOR and MICRO must not change.
    
### Upgrade Rules Examples

```json
  "groupId:artifactId": {
    "PREFIX": "1.2.3"  
  }
```
Matches dependency versions "1.2.3", "1.2.3.4.Final", but not "1.2.4" or "1.2.30".
  
```json
  "groupId:artifactId": {
    "QUALIFIER": ["Final", "Final-jbossorg-\\d+"]  
  }
```
Matches dependency versions "1.2.Final", "1.2.3.Final-jbossorg-00001", but not "1.2" or "1.2.Beta1".

```json
  "groupId:artifactId": {
    "PREFIX": "1.2.3",
    "QUALIFIER": "SP\\d+"  
  }
```
Matches dependency versions "1.2.3.SP1", "1.2.3.4.SP10", but not "1.2.3" or "1.2.4.SP1".

```json
  "groupId:artifactId": {
    "STREAM": "MICRO",  
    "QUALIFIER": "Final"  
  }
```
If an original dependency version is "1.2.3.Final", the rule matches candidate versions "1.2.4.Final", "1.2.3.4.Final",
but not "1.3.0.Final" or "1.2.3.Beta1".

## Limitations

* When using CLI tool on a multi-module project, only the single POM file specified in the "-c" parameter is processed.
  Parent or nested POMs are not. This behaviour is considered "good enough" for now, as most projects have dependency
  versions managed in a BOM or a parent POM. 
* Dependencies defined in profiles are not processed.
* Plugins are not processed.
