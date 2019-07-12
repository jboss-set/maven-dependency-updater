# Maven Dependency Updater

Experimental tool that can upgrade Maven project's dependencies according to defined rules.

Dependency versions for upgrading are searched for in the Maven Central repository.

## Configuration

A configuration file containing alignment rules must be prepared.

Example `configuration.json`:

```json
{
  "streams": {
    "commons-cli:commons-cli": "MICRO",
    "junit:junit": "MINOR",
    "org.picketlink:*": "QUALIFIER"
  }
}

```

* `streams` map:
  
  Contains keys in the format "groupId:artifactId". `groupId` and `artifactId` can be a wildcard value "*".

  Values must be one of:
  
  * `MAJOR` - upgrade to the latest MAJOR version.
  * `MINOR` - upgrade to the latest MINOR version, MAJOR must not change.
  * `MICRO` - upgrade to the latest MICRO version, MAJOR and MINOR must not change.
  * `QUALIFIER` - upgrade to the latest QUALIFIER version, MAJOR, MINOR and MICRO must not change.
  
  Default stream is `MICRO`.

## Examples

### Running Dependency Updater on a Project

```bash
java -jar $UPDATER_CLI_JAR -c configuration.json -f path/to/pom.xml
```
