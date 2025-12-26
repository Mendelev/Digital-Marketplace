# CI/CD Pipeline Template Guide

## Overview

This guide helps you create individual Harness CI pipelines for each microservice in the Digital Marketplace project. Each service gets its own pipeline instance that builds and deploys to JFrog Artifactory.

## Quick Start

1. **Review Prerequisites** - Ensure your service meets the requirements
2. **Choose Your Service** - Use one of the concrete examples below
3. **Create Pipeline** - Manually create in Harness UI using the template
4. **Test Deployment** - Run the pipeline and verify artifact in JFrog

---

## Prerequisites

### 1. Service POM Configuration

Your service's `pom.xml` **must** be updated to support dynamic versioning:

**Current (static version):**
```xml
<project>
    <groupId>com.marketplace</groupId>
    <artifactId>auth-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    ...
</project>
```

**Required (dynamic version):**
```xml
<project>
    <groupId>com.marketplace</groupId>
    <artifactId>auth-service</artifactId>
    <version>${revision}</version>
    
    <properties>
        <revision>1.0.0-SNAPSHOT</revision>  <!-- Fallback when -Drevision not provided -->
        <java.version>21</java.version>
        ...
    </properties>
    ...
</project>
```

**Why?** The pipeline passes `-Drevision=1.0.0-<buildNumber>` to generate unique versions per build.

### 2. Shared-DTOs Dependency

If your service depends on `common/shared-dtos`, deploy it first:

```xml
<!-- If your pom.xml has this dependency: -->
<dependency>
    <groupId>com.marketplace</groupId>
    <artifactId>shared-dtos</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Action Required:**
- Deploy shared-dtos to JFrog Artifactory using `shared-dtos-pipeline.yaml`
- Verify artifact exists before building dependent services

**Services that depend on shared-dtos:**
- catalog-service
- order-service
- auth-service

### 3. Harness Configuration

**Secrets Required:**
| Secret Name | Description | Example |
|------------|-------------|---------|
| `jfrog_username` | JFrog Artifactory username | `admin` or `ci-user@company.com` |
| `jfrog_token` | JFrog API token or password | `AKC...xyz` |

**GitHub Connector:**
- Must have access to your Digital Marketplace repository
- Update `connectorRef` in pipeline YAML with your connector ID

---

## Template Structure

The template (`pipeline-template-microservice.yaml`) contains:

```yaml
pipeline:
  variables:
    - serviceName       # e.g., "auth-service"
    - artifactId        # e.g., "auth-service"
    - jfrogBaseUrl      # e.g., "https://jfrog.company.com/artifactory"
    - jfrogRepoName     # e.g., "libs-snapshot-local"
    - dynamicVersion    # e.g., "1.0.0-<+pipeline.sequenceId>"
  
  stages:
    - Build_and_Deploy:
        steps:
          - Generate_Maven_Settings    # Creates settings.xml with credentials
          - Unit_Tests                 # Runs mvn test with dynamic version
          - Deploy_to_Artifactory      # Deploys artifact with altDeploymentRepository
```

---

## Concrete Examples

### Example 1: Auth Service

**Pipeline Configuration:**
```yaml
name: auth_service_Build_Deploy
identifier: auth_service_build_deploy
projectIdentifier: digital_marketplace

variables:
  serviceName: auth-service
  artifactId: auth-service
  jfrogBaseUrl: https://mycompany.jfrog.io/artifactory
  jfrogRepoName: libs-snapshot-local
  dynamicVersion: 1.0.0-<+pipeline.sequenceId>
```

**Expected Artifact:**
- Repository: `https://mycompany.jfrog.io/artifactory/libs-snapshot-local`
- Artifact: `com.marketplace:auth-service:1.0.0-42`
- Filename: `auth-service-1.0.0-42.jar`

**POM Update Required:**
```bash
cd auth-service
# Edit pom.xml:
# - Change <version>1.0.0-SNAPSHOT</version> to <version>${revision}</version>
# - Add <properties><revision>1.0.0-SNAPSHOT</revision></properties>
```

---

### Example 2: Cart Service

**Pipeline Configuration:**
```yaml
name: cart_service_Build_Deploy
identifier: cart_service_build_deploy
projectIdentifier: digital_marketplace

variables:
  serviceName: cart-service
  artifactId: cart-service
  jfrogBaseUrl: https://artifactory.internal.company.com/artifactory
  jfrogRepoName: maven-snapshots
  dynamicVersion: 1.0.0-<+pipeline.sequenceId>
```

**Notes:**
- Cart service uses on-premises JFrog (different URL pattern)
- Repository name is `maven-snapshots` (customize per your setup)
- Does NOT depend on shared-dtos

---

### Example 3: Catalog Service

**Pipeline Configuration:**
```yaml
name: catalog_service_Build_Deploy
identifier: catalog_service_build_deploy
projectIdentifier: digital_marketplace

variables:
  serviceName: catalog-service
  artifactId: catalog-service
  jfrogBaseUrl: https://jfrog.company.com/artifactory
  jfrogRepoName: digital-marketplace-snapshots
  dynamicVersion: 1.0.0-<+pipeline.sequenceId>
```

**Special Requirements:**
- **MUST** deploy shared-dtos first (catalog has this dependency)
- Uses Kafka and Spring Cloud (longer build time)
- Verify `shared-dtos-1.0.0.jar` exists in JFrog before building

---

## Step-by-Step Pipeline Creation

### Step 1: Prepare Your Service

```bash
# 1. Navigate to service directory
cd auth-service  # or cart-service, catalog-service, etc.

# 2. Backup current pom.xml
cp pom.xml pom.xml.backup

# 3. Edit pom.xml - update version
# Change:
#   <version>1.0.0-SNAPSHOT</version>
# To:
#   <version>${revision}</version>

# 4. Add revision property (in <properties> section)
# Add:
#   <revision>1.0.0-SNAPSHOT</revision>

# 5. Test locally (optional)
mvn clean test -Drevision=1.0.0-TEST
```

### Step 2: Deploy Shared-DTOs (If Required)

**Check if your service needs this:**
```bash
grep -r "shared-dtos" pom.xml
# If found, proceed:
```

**Deploy shared-dtos:**
1. Use `shared-dtos-pipeline.yaml` template
2. Run pipeline manually
3. Verify artifact in JFrog Artifactory:
   - `com.marketplace:shared-dtos:1.0.0`

### Step 3: Create Pipeline in Harness

**Manual Steps in Harness UI:**

1. **Navigate:** Harness → Pipelines → New Pipeline
2. **Choose:** Remote → YAML
3. **Copy:** Content from `pipeline-template-microservice.yaml`
4. **Replace Placeholders:**
   - `<SERVICE_NAME>` → `auth_service` (or your service)
   - `<service_name>` → `auth_service`
   - `<YOUR_PROJECT_ID>` → Your Harness project ID
   - `<SERVICE_DIRECTORY>` → `auth-service`
   - `<ARTIFACT_ID>` → `auth-service`
   - `<JFROG_BASE_URL>` → Your JFrog URL
   - `<YOUR_GITHUB_CONNECTOR>` → Your connector ID

5. **Save:** Save pipeline in Harness

### Step 4: Configure Secrets

**In Harness → Project Settings → Secrets:**

| Secret | Type | Value |
|--------|------|-------|
| jfrog_username | Text | Your JFrog username |
| jfrog_token | Text | Your JFrog API token |

**Test Connection:**
```bash
# Verify credentials work:
curl -u "USERNAME:TOKEN" \
  "https://yourcompany.jfrog.io/artifactory/api/system/ping"
# Expected: "OK"
```

### Step 5: Run Pipeline

1. **Trigger:** Manual run in Harness UI
2. **Monitor:** Watch each step:
   - ✅ Generate_Maven_Settings
   - ✅ Unit_Tests (check test reports)
   - ✅ Deploy_to_Artifactory
3. **Verify:** Check JFrog Artifactory:
   - Navigate to repository: `libs-snapshot-local`
   - Find artifact: `com/marketplace/auth-service/1.0.0-42/`
   - Confirm file: `auth-service-1.0.0-42.jar`

---

## Variable Reference

### serviceName
- **Description:** Service directory name in the repository
- **Format:** Lowercase with hyphens
- **Examples:** `auth-service`, `cart-service`, `catalog-service`
- **Used In:** `cd <serviceName> && mvn ...`

### artifactId
- **Description:** Maven artifactId from pom.xml
- **Format:** Typically matches serviceName
- **Examples:** `auth-service`, `cart-service`
- **Used In:** Logging and verification

### jfrogBaseUrl
- **Description:** Base URL of JFrog Artifactory instance (without repository name)
- **Cloud Format:** `https://<instance>.jfrog.io/artifactory`
- **On-Premises Format:** `https://jfrog.company.com/artifactory` or `https://artifactory.internal.local/artifactory`
- **Examples:** 
  - Cloud: `https://mycompany.jfrog.io/artifactory`
  - On-Prem: `https://artifactory.myorg.com/artifactory`
- **Used In:** Combined with jfrogRepoName for deployment URL

### jfrogRepoName
- **Description:** Repository name in JFrog for snapshot artifacts
- **Common Names:** `libs-snapshot-local`, `maven-snapshots`, `snapshots`
- **Custom:** Can use project-specific name like `digital-marketplace-snapshots`
- **Used In:** Forms complete deployment URL: `<jfrogBaseUrl>/<jfrogRepoName>`

### dynamicVersion
- **Description:** Version pattern with build number substitution
- **Default:** `1.0.0-<+pipeline.sequenceId>`
- **Result:** `1.0.0-1`, `1.0.0-2`, `1.0.0-3`, etc.
- **Customization:** 
  - Semantic: `1.2.3-<+pipeline.sequenceId>`
  - Date-based: `2025.12-<+pipeline.sequenceId>`
  - Snapshot: `1.0.0-SNAPSHOT-<+pipeline.sequenceId>`
- **Used In:** `-Drevision=<dynamicVersion>` parameter

---

## JFrog Repository Patterns

### Cloud JFrog (SaaS)
```
URL Pattern: https://<instance>.jfrog.io/artifactory/<repo>
Example:     https://mycompany.jfrog.io/artifactory/libs-snapshot-local

Configuration:
  jfrogBaseUrl: https://mycompany.jfrog.io/artifactory
  jfrogRepoName: libs-snapshot-local
```

### On-Premises JFrog
```
URL Pattern: https://<domain>/artifactory/<repo>
Example:     https://jfrog.internal.company.com/artifactory/maven-snapshots

Configuration:
  jfrogBaseUrl: https://jfrog.internal.company.com/artifactory
  jfrogRepoName: maven-snapshots
```

### Repository Types
| Type | Purpose | Example Name |
|------|---------|--------------|
| Snapshots | Development builds | `libs-snapshot-local` |
| Releases | Production artifacts | `libs-release-local` |
| Virtual | Aggregation | `maven-virtual` |

---

## Troubleshooting

### Issue: "Could not transfer artifact" during deployment

**Symptoms:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-deploy-plugin:3.1.1:deploy
[ERROR] Failed to transfer artifact com.marketplace:auth-service:jar:1.0.0-42
```

**Solutions:**
1. **Check secrets:**
   ```bash
   # Verify in Harness → Secrets
   # Test credentials:
   curl -u "USERNAME:TOKEN" "https://jfrog.company.com/artifactory/api/system/ping"
   ```

2. **Verify repository exists:**
   - Login to JFrog UI
   - Navigate to Repositories
   - Confirm `libs-snapshot-local` (or your repo) exists
   - Check deployment permissions

3. **Check URL format:**
   - Ensure no trailing slashes in `jfrogBaseUrl`
   - Verify repository name is correct

---

### Issue: "Property 'revision' not found"

**Symptoms:**
```
[ERROR] Failed to execute goal on project auth-service
[ERROR] Property 'revision' is not defined
```

**Solution:**
```xml
<!-- Add to pom.xml in <properties> section: -->
<properties>
    <revision>1.0.0-SNAPSHOT</revision>
    <!-- ... other properties ... -->
</properties>

<!-- Update version to: -->
<version>${revision}</version>
```

---

### Issue: Missing shared-dtos dependency

**Symptoms:**
```
[ERROR] Failed to execute goal on project catalog-service
[ERROR] Could not resolve dependencies for project com.marketplace:catalog-service
[ERROR] Could not find artifact com.marketplace:shared-dtos:jar:1.0.0
```

**Solution:**
1. Deploy shared-dtos first:
   - Use `shared-dtos-pipeline.yaml`
   - Verify deployment in JFrog
2. Check shared-dtos version in service pom.xml matches deployed version
3. Verify JFrog repository is accessible to Maven

---

### Issue: Wrong artifact version in JFrog

**Symptoms:**
- Artifact shows version `1.0.0-SNAPSHOT` instead of `1.0.0-42`
- Multiple builds overwrite the same artifact

**Solution:**
1. **Check Maven commands include `-Drevision`:**
   ```bash
   # Should be:
   mvn test -Drevision=1.0.0-<+pipeline.sequenceId>
   mvn deploy -Drevision=1.0.0-<+pipeline.sequenceId>
   ```

2. **Verify dynamicVersion variable:**
   ```yaml
   variables:
     - name: dynamicVersion
       value: 1.0.0-<+pipeline.sequenceId>  # Must have <+pipeline.sequenceId>
   ```

3. **Check pom.xml uses ${revision}:**
   ```xml
   <version>${revision}</version>
   ```

---

### Issue: Tests fail in pipeline but pass locally

**Common Causes:**

1. **Environment differences:**
   - Pipeline uses Maven 3.9 + JDK 21
   - Check local versions match

2. **Missing test resources:**
   - Verify `src/test/resources` files are committed
   - Check `.gitignore` doesn't exclude test files

3. **Database not available:**
   - Pipeline tests should use H2/embedded DB
   - Don't rely on external Postgres in unit tests

**Debug Steps:**
```bash
# Test locally with same environment:
docker run --rm -v "$PWD":/app -w /app/auth-service \
  maven:3.9-eclipse-temurin-21 \
  mvn clean test -Drevision=1.0.0-TEST
```

---

## Deployment Checklist

Use this checklist when creating a new pipeline:

### Pre-Deployment
- [ ] Service POM uses `<version>${revision}</version>`
- [ ] Service POM has `<revision>1.0.0-SNAPSHOT</revision>` in properties
- [ ] shared-dtos deployed (if service depends on it)
- [ ] JFrog repository exists and is accessible
- [ ] Harness secrets configured: `jfrog_username`, `jfrog_token`
- [ ] GitHub connector configured and accessible

### Pipeline Configuration
- [ ] Replaced `<SERVICE_NAME>` with actual service name
- [ ] Replaced `<service_name>` identifier
- [ ] Replaced `<YOUR_PROJECT_ID>` with Harness project ID
- [ ] Set `serviceName` variable (e.g., "auth-service")
- [ ] Set `artifactId` variable (e.g., "auth-service")
- [ ] Set `jfrogBaseUrl` variable (without trailing slash)
- [ ] Set `jfrogRepoName` variable
- [ ] Set `dynamicVersion` variable with `<+pipeline.sequenceId>`
- [ ] Updated `connectorRef` with GitHub connector ID

### Post-Deployment
- [ ] Pipeline runs successfully
- [ ] All three steps complete: Settings → Tests → Deploy
- [ ] Test reports generated
- [ ] Artifact visible in JFrog Artifactory
- [ ] Artifact version format correct (e.g., 1.0.0-42)
- [ ] Artifact downloadable from JFrog

---

## Next Steps

After setting up your first pipeline:

1. **Create pipelines for other services:**
   - Repeat process for each microservice
   - Use same JFrog configuration
   - Adjust only `serviceName` and `artifactId`

2. **Set up automated triggers:**
   - Configure GitHub webhooks in Harness
   - Trigger on push to `main` branch
   - Filter by service directory changes

3. **Add release pipeline:**
   - Modify template to use `libs-release-local` repository
   - Change version pattern to remove SNAPSHOT
   - Add approval steps

4. **Monitor and optimize:**
   - Review build times
   - Check test coverage reports
   - Monitor JFrog storage usage

---

## Reference Files

| File | Purpose |
|------|---------|
| `pipeline-template-microservice.yaml` | Main pipeline template |
| `shared-dtos-pipeline.yaml` | Pipeline for common/shared-dtos |
| `PIPELINE_TEMPLATE_GUIDE.md` | This guide (usage documentation) |
| `POM_REQUIREMENTS.md` | Detailed POM configuration guide |

---

## Support

For issues or questions:
1. Review troubleshooting section above
2. Check Harness pipeline execution logs
3. Verify JFrog Artifactory logs
4. Test Maven commands locally first

**Common Resources:**
- Harness Docs: https://developer.harness.io/docs/continuous-integration
- JFrog Maven: https://jfrog.com/help/r/jfrog-artifactory-documentation/maven-repository
- Maven Deploy Plugin: https://maven.apache.org/plugins/maven-deploy-plugin/
