
## Examples

### Running Dependency Updater on a Project

```bash
java -jar target/maven-version-updater-0.1-SNAPSHOT.jar -c configuration.json -o bom.xml -d dependencies.txt
```

### Print All Project Dependencies with PME

```bash
java -jar $PME_CLI_JAR -p -f ../wildfly/pom.xml
```

### Aligning Dependencies on a Project with PME

```bash
java -jar $PME_CLI_JAR \
     -DdependencyManagement=org.wildfly:alignment-bom:0.0.1 \
     -DstrictAlignment=false \
     -f ../wildfly/pom.xml
```
