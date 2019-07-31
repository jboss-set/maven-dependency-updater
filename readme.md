# Maven Dependency Updater

Experimental tool that upgrades Maven project's dependencies according to defined rules.

Dependency versions for upgrading are searched for in maven repositories (currently only Maven Central).

## Configuration

A configuration file containing alignment rules must be prepared.

Example `configuration.json`:

```json
{
  "ignoreScopes": ["test"],
  "rules": {
    "commons-cli:commons-cli": "MICRO",
    "org.picketlink:*": {
      "PREFIX": "2.5.5",
      "QUALIFIER": "SP\\d+"
    }
  }
}

```
* `ignoreScopes`: A list of maven dependency scopes that should be ignored.
* `rules`: A map where keys are of the format "groupId:artifactId" and values are _alignment rules_.

  `groupId` and `artifactId` can be a wildcard "*".

  _Alignment rule_ is either:
  
  * a string, in which case it can have following values:
    * `NEVER` - do not upgrade this G:A.
    * a _stream name_.
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
Matches candidate versions "1.2.3", "1.2.3.4.Final", but not "1.2.4" or "1.2.30".
  
```json
  "groupId:artifactId": {
    "QUALIFIER": ["Final", "Final-jbossorg-\\d+"]  
  }
```
Matches candidate versions "1.2.Final", "1.2.3.Final-jbossorg-00001", but not "1.2" or "1.2.Beta1".

```json
  "groupId:artifactId": {
    "PREFIX": "1.2.3",
    "QUALIFIER": "SP\\d+"  
  }
```
Matches candidate versions "1.2.3.SP1", "1.2.3.4.SP10", but not "1.2.3" or "1.2.4.SP1".

```json
  "groupId:artifactId": {
    "STREAM": "MICRO",  
    "QUALIFIER": "Final"  
  }
```
If an original dependency version is "1.2.3.Final", the rule matches candidate versions "1.2.4.Final", "1.2.3.4.Final",
but not "1.3.0.Final" or "1.2.3.Beta1".
  
## Limitations

* In a multi-module project, currently only the top level module dependencies are upgraded.
* Dependencies defined in profiles are not upgraded.
* Plugins are not upgraded.

## Examples

Align a project:

```bash
$ java -jar $CLI_JAR align -c path/to/configuration.json -f path/to/pom.xml
```

Attempts to generate a sane default configuration for a project, which should then be reviewed and modified as needed:

```bash
$ java -jar $CLI_JAR generate-config -c path/to/configuration.json -f path/to/pom.xml
```

Check if version prefixes defined in configuration file still match versions in the pom (i.e. detects if the project
has moved forward with dependency versions, and the configuration needs to be updated): 

```bash
$ java -jar $CLI_JAR check-config -c path/to/configuration.json -f path/to/pom.xml
Dependency org.jboss.spec.javax.faces:jboss-jsf-api_2.3_spec:jar:2.3.9.SP02 doesn't match prefix '2.3.8'
...
```
