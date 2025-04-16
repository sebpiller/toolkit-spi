
# Goodies

## Find outdated dependencies

Use a custom config in settings.xml for system-wide effect:

```xml

<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <profiles>
        <profile>
            <id>default-values</id>
            <properties>
                <maven.versions.ignore>(?i).*[\.-]?(alpha|beta|rc|preview|SNAPSHOT|M\d+)[\.-]?.*</maven.versions.ignore>
                <processDependencyManagementTransitive>false</processDependencyManagementTransitive>
            </properties>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>default-values</activeProfile>
        ...
    </activeProfiles>
</settings>
```

Then simply invoke:

```shell
mvn versions:display-dependency-updates versions:display-property-updates versions:display-plugin-updates
```

... to get a clear report of updatable dependencies, ignoring snapshots, beta, rc, etc...