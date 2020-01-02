# Maven Dependency Updater

Experimental tool that reports possible dependency upgrades in a Maven project. The dependency upgrades can also be
directly performed in the POM file, or a pull requests can be created.

It is possible to define per dependency "alignment rules" to limit which component upgrades should be reported - e.g.
only to newer micro versions or to versions with certain qualifiers. 

It is also possible to specify maven repositories which should be used to look up new dependency versions
(Maven Central by default).

## Usage

```bash
java -jar <path/to/alignment-cli.jar> <action> -c <path/co/configuration.json> -f <path/to/pom.xml>
```

- alignment-cli.jar file is generated during build in `$SOURCE_DIR/cli/target/`,
- `<action>` can be "generate-report" or "perform-upgrades".

### Usage Examples

Generate text report with possible dependency upgrades:

```bash
$ java -jar $CLI_JAR generate-report -c path/to/configuration.json -f path/to/pom.xml
```

Perform possible dependency upgrades in the POM:

```bash
$ java -jar $CLI_JAR perform-upgrades -c path/to/configuration.json -f path/to/pom.xml
```

## Configuration

A configuration file containing alignment rules must be prepared.

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
* `rules`: A map where keys are of the format "groupId:artifactId" and values are _alignment rules_.

  `groupId` and `artifactId` can be a wildcard "*".

  _Alignment rule_ is either:
  
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
    
### Alignment Rules Examples

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

* In a multi-module project, only the single POM file specified in "-c" parameter is processed, no parent or nested POMs. 
  I.e. the tool needs to be run separately for each POM which needs to be processed.
* Dependencies defined in profiles are not processed.
* Plugins are not processed.
