# ArchUnit Reference

Copy `ArchitectureRules.java` into the project's test source tree and replace
`com.company` with the real root package.

The rules are intentionally minimal. Add feature-specific rules only when a
real architectural constraint exists.

Recommended Maven test dependency:

```xml
<dependency>
  <groupId>com.tngtech.archunit</groupId>
  <artifactId>archunit-junit5</artifactId>
  <scope>test</scope>
</dependency>
```
