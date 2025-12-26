# Auth Service Pipeline Configuration

## Pipeline Details

- **Name:** auth_service_Build_Deploy
- **Identifier:** auth_service_build_deploy
- **Service:** auth-service
- **Location:** `.harness/auth-service-pipeline.yaml`

## Before Using This Pipeline

### 1. Update auth-service POM

The auth-service `pom.xml` must use dynamic versioning:

```bash
cd auth-service
# Edit pom.xml and update:
# - Change <version>1.0.0-SNAPSHOT</version> to <version>${revision}</version>
# - Add <revision>1.0.0-SNAPSHOT</revision> in <properties> section
```

See [POM_REQUIREMENTS.md](../POM_REQUIREMENTS.md) for detailed instructions.

### 2. Configure Harness Secrets

In your Harness project, create these secrets:

| Secret Name | Value | Description |
|------------|-------|-------------|
| `jfrog_username` | Your JFrog username | Authentication username |
| `jfrog_token` | Your JFrog API token | Authentication token/password |

### 3. Update Pipeline Variables

In the pipeline YAML, update these values:

```yaml
# Line 3: Update project identifier if different
projectIdentifier: javatodoapp  # Your Harness project ID

# Line 27: Update JFrog base URL
value: https://yourcompany.jfrog.io/artifactory  # Your JFrog instance

# Line 34: Update repository name if different
value: libs-snapshot-local  # Your snapshot repository

# Line 44: Update GitHub connector if different
connectorRef: githubmendelev  # Your GitHub connector ID
```

## Pipeline Variables

| Variable | Value | Description |
|----------|-------|-------------|
| serviceName | `auth-service` | Service directory name |
| artifactId | `auth-service` | Maven artifactId |
| jfrogBaseUrl | ⚠️ UPDATE | Your JFrog Artifactory URL |
| jfrogRepoName | `libs-snapshot-local` | JFrog repository name |
| dynamicVersion | `1.0.0-<+pipeline.sequenceId>` | Version pattern |

## Pipeline Steps

### Step 1: Generate Maven Settings
- Creates `~/.m2/settings.xml` with JFrog credentials
- Configures `altDeploymentRepository` for deployment

### Step 2: Unit Tests
- Runs `mvn test` with dynamic version
- Generates JUnit test reports
- Reports available in Harness UI

### Step 3: Deploy to Artifactory
- Builds and deploys artifact to JFrog
- Uses `-DaltDeploymentRepository` to override deployment target
- Skips tests (already ran in Step 2)

## Expected Output

### Artifact Details
- **Group ID:** `com.marketplace`
- **Artifact ID:** `auth-service`
- **Version:** `1.0.0-1`, `1.0.0-2`, etc. (increments with each build)
- **File:** `auth-service-1.0.0-<buildNumber>.jar`

### JFrog Location
```
<jfrogBaseUrl>/<jfrogRepoName>/
  └── com/marketplace/auth-service/
      ├── 1.0.0-1/
      │   ├── auth-service-1.0.0-1.jar
      │   └── auth-service-1.0.0-1.pom
      ├── 1.0.0-2/
      │   ├── auth-service-1.0.0-2.jar
      │   └── auth-service-1.0.0-2.pom
      └── ...
```

## Usage in Harness

### Option 1: Import via UI
1. Navigate to Harness → Pipelines
2. Click "Create Pipeline" → "Import From Git"
3. Select `.harness/auth-service-pipeline.yaml`
4. Update variables as needed
5. Save and run

### Option 2: Copy-Paste
1. Navigate to Harness → Pipelines
2. Click "Create Pipeline" → "YAML"
3. Copy content from `.harness/auth-service-pipeline.yaml`
4. Paste and update variables
5. Save and run

## Testing Locally

Before running the pipeline, test the build locally:

```bash
cd auth-service

# Test with default version
mvn clean package
# Should build: auth-service-1.0.0-SNAPSHOT.jar

# Test with dynamic version (simulates pipeline)
mvn clean package -Drevision=1.0.0-TEST
# Should build: auth-service-1.0.0-TEST.jar
```

## Troubleshooting

### Pipeline fails at Unit Tests

**Check:**
- Tests pass locally: `cd auth-service && mvn test`
- POM has `${revision}` property configured
- No external dependencies (database) required for unit tests

### Pipeline fails at Deploy

**Check:**
- JFrog secrets configured correctly
- JFrog URL is accessible from Harness Cloud runners
- Repository `libs-snapshot-local` exists in JFrog
- JFrog user has deploy permissions

### Artifact has wrong version

**Check:**
- POM uses `<version>${revision}</version>`
- POM has default `<revision>1.0.0-SNAPSHOT</revision>` in properties
- Pipeline passes `-Drevision` to all Maven commands

## Next Steps

After successful deployment:

1. **Verify in JFrog:**
   - Login to JFrog Artifactory
   - Navigate to `libs-snapshot-local`
   - Find: `com/marketplace/auth-service/1.0.0-<buildNumber>/`

2. **Create pipelines for other services:**
   - Copy `.harness/auth-service-pipeline.yaml`
   - Update service name and artifact ID
   - Repeat for cart-service, catalog-service, etc.

3. **Set up triggers:**
   - Configure GitHub webhook in Harness
   - Trigger on push to main branch
   - Filter by `auth-service/**` path

## Related Files

- [../pipeline-template-microservice.yaml](../pipeline-template-microservice.yaml) - Template used to create this pipeline
- [../PIPELINE_TEMPLATE_GUIDE.md](../PIPELINE_TEMPLATE_GUIDE.md) - Comprehensive guide
- [../POM_REQUIREMENTS.md](../POM_REQUIREMENTS.md) - POM configuration details
- [../auth-service/pom.xml](../auth-service/pom.xml) - Service POM that needs updating
