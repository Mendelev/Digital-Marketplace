# POM Configuration Requirements

## Overview

This document explains how to configure your service's `pom.xml` to work with the CI/CD pipeline template. All microservices must adopt dynamic versioning using Maven's `${revision}` property.

---

## Quick Reference

**Before (Current - Static Version):**
```xml
<version>1.0.0-SNAPSHOT</version>
```

**After (Required - Dynamic Version):**
```xml
<version>${revision}</version>
<properties>
    <revision>1.0.0-SNAPSHOT</revision>
</properties>
```

---

## Why Dynamic Versioning?

### Problem with Static Versions

```xml
<version>1.0.0-SNAPSHOT</version>
```

**Issues:**
- Every build overwrites the same artifact in JFrog
- Cannot track which build produced which artifact
- Impossible to roll back to specific builds
- SNAPSHOT versions are mutable (bad for reproducibility)

### Solution: Maven CI Friendly Versions

```xml
<version>${revision}</version>
```

**Benefits:**
- Each build gets unique version: `1.0.0-1`, `1.0.0-2`, `1.0.0-3`
- Artifacts are immutable and traceable
- Easy rollback to specific build numbers
- Pipeline controls version via `-Drevision=1.0.0-42`

---

## Step-by-Step Migration

### Step 1: Backup Current POM

```bash
cd auth-service  # or your service
cp pom.xml pom.xml.backup
```

### Step 2: Update Version Element

**Find this section:**
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.marketplace</groupId>
    <artifactId>auth-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>  <!-- CHANGE THIS -->
    <packaging>jar</packaging>
```

**Change to:**
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.marketplace</groupId>
    <artifactId>auth-service</artifactId>
    <version>${revision}</version>  <!-- CHANGED -->
    <packaging>jar</packaging>
```

### Step 3: Add Revision Property

**Find the `<properties>` section:**
```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.2.1</spring-boot.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

**Add `revision` property at the top:**
```xml
<properties>
    <revision>1.0.0-SNAPSHOT</revision>  <!-- ADD THIS -->
    
    <java.version>21</java.version>
    <spring-boot.version>3.2.1</spring-boot.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### Step 4: Test Locally

```bash
# Test with default version (from property)
mvn clean package
# Should build: auth-service-1.0.0-SNAPSHOT.jar

# Test with override version (simulating pipeline)
mvn clean package -Drevision=1.0.0-TEST
# Should build: auth-service-1.0.0-TEST.jar

# Test with dynamic version (pipeline pattern)
mvn clean package -Drevision=1.0.0-42
# Should build: auth-service-1.0.0-42.jar
```

### Step 5: Verify Output

```bash
ls -lh target/*.jar
# Expected: auth-service-1.0.0-42.jar (or your test version)

# Check the JAR manifest
unzip -p target/auth-service-1.0.0-42.jar META-INF/MANIFEST.MF | grep Implementation-Version
# Expected: Implementation-Version: 1.0.0-42
```

---

## Complete Example

### Before: auth-service/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.1</version>
        <relativePath/>
    </parent>

    <groupId>com.marketplace</groupId>
    <artifactId>auth-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>Auth Service</name>
    <description>Authentication and authorization service</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Dependencies here -->
    </dependencies>

    <build>
        <plugins>
            <!-- Plugins here -->
        </plugins>
    </build>
</project>
```

### After: auth-service/pom.xml (Dynamic Versioning)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.1</version>
        <relativePath/>
    </parent>

    <groupId>com.marketplace</groupId>
    <artifactId>auth-service</artifactId>
    <version>${revision}</version>  <!-- ✅ CHANGED -->
    <packaging>jar</packaging>
    <name>Auth Service</name>
    <description>Authentication and authorization service</description>

    <properties>
        <revision>1.0.0-SNAPSHOT</revision>  <!-- ✅ ADDED -->
        
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Dependencies here -->
    </dependencies>

    <build>
        <plugins>
            <!-- Plugins here -->
        </plugins>
    </build>
</project>
```

---

## Service-Specific Examples

### Auth Service

```xml
<groupId>com.marketplace</groupId>
<artifactId>auth-service</artifactId>
<version>${revision}</version>

<properties>
    <revision>1.0.0-SNAPSHOT</revision>
    <java.version>21</java.version>
    <!-- ... other properties ... -->
</properties>
```

**Pipeline builds:**
- Build 1: `auth-service-1.0.0-1.jar`
- Build 2: `auth-service-1.0.0-2.jar`
- Build 42: `auth-service-1.0.0-42.jar`

---

### Cart Service

```xml
<groupId>com.marketplace</groupId>
<artifactId>cart-service</artifactId>
<version>${revision}</version>

<properties>
    <revision>1.0.0-SNAPSHOT</revision>
    <java.version>21</java.version>
    <!-- ... other properties ... -->
</properties>
```

**Pipeline builds:**
- Build 1: `cart-service-1.0.0-1.jar`
- Build 5: `cart-service-1.0.0-5.jar`

---

### Catalog Service (with Spring Cloud)

```xml
<groupId>com.marketplace</groupId>
<artifactId>catalog-service</artifactId>
<version>${revision}</version>

<properties>
    <revision>1.0.0-SNAPSHOT</revision>
    
    <java.version>21</java.version>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
    <!-- ... other properties ... -->
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## Shared-DTOs Configuration

The `common/shared-dtos` module uses a **stable version** (not dynamic):

```xml
<groupId>com.marketplace</groupId>
<artifactId>shared-dtos</artifactId>
<version>${revision}</version>

<properties>
    <revision>1.0.0</revision>  <!-- NOTE: No -SNAPSHOT, stable version -->
    
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

**Why stable version?**
- Shared library should have predictable versions
- Services depend on specific shared-dtos version
- Easier to manage breaking changes via version bumps

**Versioning strategy:**
| Change Type | Version Update | Example |
|------------|----------------|---------|
| Bug fix | Patch | 1.0.0 → 1.0.1 |
| New DTO | Minor | 1.0.1 → 1.1.0 |
| Breaking change | Major | 1.1.0 → 2.0.0 |

---

## How Pipeline Uses Revision

### During Build

```bash
# Pipeline executes:
cd auth-service
mvn clean test -Drevision=1.0.0-42

# Maven resolves:
# ${revision} → 1.0.0-42
# Final artifact: auth-service-1.0.0-42.jar
```

### During Deployment

```bash
# Pipeline executes:
cd auth-service
mvn deploy -Drevision=1.0.0-42 -DaltDeploymentRepository=...

# Deploys to JFrog:
# com.marketplace:auth-service:1.0.0-42
```

### In JFrog Artifactory

```
Repository: libs-snapshot-local/
  └── com/
      └── marketplace/
          └── auth-service/
              ├── 1.0.0-1/
              │   ├── auth-service-1.0.0-1.jar
              │   └── auth-service-1.0.0-1.pom
              ├── 1.0.0-2/
              │   ├── auth-service-1.0.0-2.jar
              │   └── auth-service-1.0.0-2.pom
              └── 1.0.0-42/
                  ├── auth-service-1.0.0-42.jar
                  └── auth-service-1.0.0-42.pom
```

---

## Troubleshooting

### Issue: "Could not resolve placeholder 'revision'"

**Symptom:**
```
[ERROR] Some problems were encountered while processing the POMs:
[ERROR] 'version' contains an expression but should be a constant.
```

**Cause:** Missing `<revision>` property in `<properties>` section.

**Solution:**
```xml
<properties>
    <revision>1.0.0-SNAPSHOT</revision>  <!-- Add this -->
    <!-- ... other properties ... -->
</properties>
```

---

### Issue: JAR still has SNAPSHOT version

**Symptom:**
```bash
ls target/
# Shows: auth-service-1.0.0-SNAPSHOT.jar
# Expected: auth-service-1.0.0-42.jar
```

**Cause:** Build command missing `-Drevision` parameter.

**Solution:**
```bash
# Wrong:
mvn clean package

# Correct:
mvn clean package -Drevision=1.0.0-42
```

---

### Issue: IntelliJ shows "Cannot resolve ${revision}"

**Symptom:** IDE shows red underline on `<version>${revision}</version>`

**Cause:** IntelliJ needs to reimport Maven project.

**Solution:**
1. Right-click `pom.xml` → Maven → Reload Project
2. Or: View → Tool Windows → Maven → Click refresh icon
3. Restart IntelliJ if issue persists

---

### Issue: Dependency version mismatch

**Symptom:**
```
[ERROR] Failed to collect dependencies at com.marketplace:shared-dtos:jar:1.0.0-42
[ERROR] Failed to read artifact descriptor for com.marketplace:shared-dtos:jar:1.0.0-42
```

**Cause:** Service references dynamic version of shared-dtos (wrong).

**Solution:**
```xml
<!-- Wrong: Dynamic version for shared dependency -->
<dependency>
    <groupId>com.marketplace</groupId>
    <artifactId>shared-dtos</artifactId>
    <version>${revision}</version>  <!-- DON'T DO THIS -->
</dependency>

<!-- Correct: Fixed version for shared dependency -->
<dependency>
    <groupId>com.marketplace</groupId>
    <artifactId>shared-dtos</artifactId>
    <version>1.0.0</version>  <!-- Use stable version -->
</dependency>
```

---

## Validation Checklist

Before running pipeline, verify your POM:

- [ ] `<version>${revision}</version>` instead of `<version>1.0.0-SNAPSHOT</version>`
- [ ] `<properties>` section contains `<revision>1.0.0-SNAPSHOT</revision>`
- [ ] Local build works: `mvn clean package`
- [ ] Override build works: `mvn clean package -Drevision=1.0.0-TEST`
- [ ] Output JAR has correct version: `ls target/*.jar`
- [ ] No IntelliJ/IDE errors after Maven reload
- [ ] Shared-dtos dependency uses fixed version (if applicable)

---

## Migration Script

Automate the POM update:

```bash
#!/bin/bash
# migrate-pom.sh - Convert service POM to dynamic versioning

SERVICE_DIR="$1"

if [ -z "$SERVICE_DIR" ]; then
    echo "Usage: ./migrate-pom.sh <service-directory>"
    echo "Example: ./migrate-pom.sh auth-service"
    exit 1
fi

POM_FILE="$SERVICE_DIR/pom.xml"

if [ ! -f "$POM_FILE" ]; then
    echo "Error: $POM_FILE not found"
    exit 1
fi

# Backup
cp "$POM_FILE" "$POM_FILE.backup"

# Replace version with ${revision}
sed -i.tmp 's|<version>1\.0\.0-SNAPSHOT</version>|<version>${revision}</version>|g' "$POM_FILE"

# Add revision property (after <properties> tag)
sed -i.tmp '/<properties>/a\
        <revision>1.0.0-SNAPSHOT</revision>
' "$POM_FILE"

# Clean up
rm "$POM_FILE.tmp"

echo "✅ Updated $POM_FILE"
echo "   Backup saved: $POM_FILE.backup"
echo ""
echo "Test the changes:"
echo "  cd $SERVICE_DIR"
echo "  mvn clean package -Drevision=1.0.0-TEST"
```

**Usage:**
```bash
chmod +x migrate-pom.sh

# Migrate single service
./migrate-pom.sh auth-service

# Migrate all services
for service in auth-service cart-service catalog-service order-service; do
    ./migrate-pom.sh "$service"
done
```

---

## Best Practices

### 1. Default Revision Value

```xml
<properties>
    <!-- Use SNAPSHOT as default for local development -->
    <revision>1.0.0-SNAPSHOT</revision>
</properties>
```

**Reasoning:**
- Local builds use SNAPSHOT (expected for development)
- Pipeline overrides with build number
- Clear distinction between local and CI builds

### 2. Version Format

```xml
<!-- Good: Semantic versioning with build number -->
<revision>1.0.0-${pipeline.sequenceId}</revision>
→ 1.0.0-1, 1.0.0-2, 1.0.0-3

<!-- Alternative: Date-based -->
<revision>2025.12-${pipeline.sequenceId}</revision>
→ 2025.12-1, 2025.12-2

<!-- Bad: No build identifier -->
<revision>1.0.0</revision>
→ All builds have same version (defeats purpose)
```

### 3. Shared Dependencies

```xml
<!-- Services should reference STABLE versions of shared-dtos -->
<dependency>
    <groupId>com.marketplace</groupId>
    <artifactId>shared-dtos</artifactId>
    <version>1.0.0</version>  <!-- NOT ${revision} -->
</dependency>
```

### 4. Multi-Module Projects

If you have a parent POM later:

```xml
<!-- Parent POM -->
<groupId>com.marketplace</groupId>
<artifactId>digital-marketplace-parent</artifactId>
<version>${revision}</version>
<packaging>pom</packaging>

<properties>
    <revision>1.0.0-SNAPSHOT</revision>
</properties>

<modules>
    <module>auth-service</module>
    <module>cart-service</module>
    <!-- ... -->
</modules>
```

```xml
<!-- Child POM (auth-service) -->
<parent>
    <groupId>com.marketplace</groupId>
    <artifactId>digital-marketplace-parent</artifactId>
    <version>${revision}</version>
</parent>

<artifactId>auth-service</artifactId>
<!-- Inherits version from parent -->
```

---

## Reference Links

- [Maven CI Friendly Versions](https://maven.apache.org/maven-ci-friendly.html)
- [Spring Boot with CI Friendly Versions](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/#using.parent-pom)
- [JFrog Maven Repository](https://jfrog.com/help/r/jfrog-artifactory-documentation/maven-repository)

---

## Summary

**Required Changes:**
1. Change `<version>1.0.0-SNAPSHOT</version>` to `<version>${revision}</version>`
2. Add `<revision>1.0.0-SNAPSHOT</revision>` to `<properties>` section
3. Test with `mvn clean package -Drevision=1.0.0-TEST`

**Pipeline Behavior:**
- Each build passes `-Drevision=1.0.0-<buildNumber>`
- Unique artifacts per build in JFrog
- Traceability and immutability

**Next Steps:**
1. Update all service POMs following this guide
2. Verify local builds work
3. Create pipelines using template
4. Deploy to JFrog and verify versions
