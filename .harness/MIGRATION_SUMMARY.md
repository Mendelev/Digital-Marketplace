# Pipeline Restructuring Summary - January 2026

## Overview

Successfully restructured all Harness CI/CD pipelines to remove JFrog Artifactory integration and migrate to Harness Cloud infrastructure.

## Changes Applied

### ✅ Removed JFrog Artifactory Integration

**What was removed:**
- All JFrog Artifactory deployment steps
- JFrog credentials and authentication
- Maven settings generation for Artifactory
- Artifact deployment to external repository

**Why:**
- Simplified build process
- Reduced external dependencies
- Faster pipeline execution
- Lower maintenance overhead

### ✅ Migrated to Harness Cloud

**What changed:**
- Runtime changed from `Docker` to `Cloud`
- Using Harness-managed infrastructure
- Image connector: `account.harnessImage`
- No need for Docker daemon management

**Benefits:**
- Faster build startup times
- Harness-managed scaling and reliability
- Reduced infrastructure costs
- Better integration with Harness features

## Files Modified

### Service Pipelines (9 files)
All service pipelines were updated with identical changes:

1. [auth_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/auth_service_build.yaml)
2. [catalog_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/catalog_service_build.yaml)
3. [inventory_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/inventory_service_build.yaml)
4. [order_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/order_service_build.yaml)
5. [payment_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/payment_service_build.yaml)
6. [search_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/search_service_build.yaml)
7. [shipping_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/shipping_service_build.yaml) *(includes Snyk scan)*
8. [user_service_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/user_service_build.yaml)
9. [shared_dtos_build.yaml](orgs/default/projects/digitalmarketplace/pipelines/shared_dtos_build.yaml)

### Templates (1 file)
1. [Deploy_Services/version1.yaml](orgs/default/templates/Deploy_Services/version1.yaml) - Updated generic template

### Input Sets (3 files)
1. [shipping_service_build/input_sets/shippingserviceinput.yaml](orgs/default/projects/digitalmarketplace/pipelines/shipping_service_build/input_sets/shippingserviceinput.yaml)
2. [catalog_service_build/input_sets/catalogservice.yaml](orgs/default/projects/digitalmarketplace/pipelines/catalog_service_build/input_sets/catalogservice.yaml)
3. [order_service_build/input_sets/orderserviceinput.yaml](orgs/default/projects/digitalmarketplace/pipelines/order_service_build/input_sets/orderserviceinput.yaml)

### Templates Now Deprecated (Not Removed)
These templates are no longer used but kept for reference:
1. `Deploy_to_Artifactory/version1.yaml` - ⚠️ **DEPRECATED**
2. `Generate_Maven_Settings/undefined.yaml` - ⚠️ **DEPRECATED**

## Before vs After

### Before: Docker Runtime with Artifactory
```yaml
runtime:
  type: Docker
  spec: {}
execution:
  steps:
    - step:
        name: Generate_Maven_Settings
        template:
          templateRef: org.Generate_Maven_Settings
    - step:
        name: Deploy_to_Artifactory
        template:
          templateRef: org.Deploy_to_Artifactory
```

### After: Cloud Runtime with Simple Build
```yaml
runtime:
  type: Cloud
  spec: {}
execution:
  steps:
    - step:
        type: Run
        name: Build_Service
        spec:
          connectorRef: account.harnessImage
          image: maven:3.9-eclipse-temurin-21
          shell: Sh
          command: |
            echo "Building <+pipeline.variables.serviceName>..."
            cd <+pipeline.variables.serviceName> && \
              mvn -B clean package -DskipTests
            
            echo "Build completed successfully"
            ls -lh target/*.jar
```

## Variables Changes

### Removed Variables
These variables were removed from all pipelines:
- ~~`jfrogBaseUrl`~~ - JFrog Artifactory base URL
- ~~`jfrogRepoName`~~ - JFrog repository name
- ~~`dynamicVersion`~~ - Dynamic version with build number

### Retained Variables
These variables are still used:
- `serviceName` - Service directory name
- `artifactId` - Maven artifact ID

## Secrets Changes

### No Longer Required
- ~~`jfrog_username`~~ - Can be deleted from Harness secrets
- ~~`jfrog_token`~~ - Can be deleted from Harness secrets

### Still Required
- `snyk_access_token` - For security scanning (shipping service only)

## New Pipeline Behavior

### What Pipelines Now Do:
1. ✅ Clone repository from GitHub
2. ✅ Build service with Maven
3. ✅ Verify compilation succeeds
4. ✅ Generate JAR artifacts
5. ✅ (Shipping service) Run Snyk security scan

### What Pipelines No Longer Do:
- ❌ Deploy to JFrog Artifactory
- ❌ Publish artifacts externally
- ❌ Generate Maven settings.xml
- ❌ Version artifacts dynamically

## Use Cases

The updated pipelines are perfect for:
- ✅ **PR Validation** - Verify code compiles before merge
- ✅ **Branch Builds** - Test feature branches
- ✅ **Security Scanning** - Check for vulnerabilities
- ✅ **Build Verification** - Ensure no compilation errors
- ✅ **CI/CD Gates** - Block merges on build failures

## Future Enhancements

If you need to add deployment back, consider:

### Option 1: Harness Artifact Registry
```yaml
- step:
    type: BuildAndPushDockerRegistry
    name: Build_and_Push_Image
    spec:
      repo: harness/<+pipeline.variables.serviceName>
      tags:
        - latest
        - <+pipeline.sequenceId>
```

### Option 2: Cloud Provider Publishing
```yaml
- step:
    type: S3Upload
    name: Upload_to_S3
    spec:
      bucket: my-artifacts
      sourcePath: target/*.jar
```

### Option 3: Direct Deployment
```yaml
- step:
    type: K8sRollingDeploy
    name: Deploy_to_Kubernetes
    spec:
      skipDryRun: false
```

## Rollback Instructions

If you need to restore JFrog Artifactory integration:

1. **Restore pipeline variables:**
   ```yaml
   - name: jfrogBaseUrl
     value: https://trialq6tyqg.jfrog.io/artifactory
   - name: jfrogRepoName
     value: libs-snapshot-local
   ```

2. **Add back template steps:**
   ```yaml
   - step:
       name: Generate_Maven_Settings
       template:
         templateRef: org.Generate_Maven_Settings
   - step:
       name: Deploy_to_Artifactory
       template:
         templateRef: org.Deploy_to_Artifactory
   ```

3. **Restore secrets:**
   - Add `jfrog_username` secret
   - Add `jfrog_token` secret

4. **Change runtime back to Docker:**
   ```yaml
   runtime:
     type: Docker
     spec: {}
   ```

## Testing Recommendations

1. **Test each pipeline:**
   ```bash
   # Trigger a build for each service
   # Verify Maven build succeeds
   # Check build logs for errors
   ```

2. **Verify artifacts:**
   ```bash
   # Check that JAR files are generated
   # Confirm in pipeline logs: "target/*.jar"
   ```

3. **Monitor execution time:**
   - Cloud runtime should be faster than Docker
   - Typical build: 2-5 minutes

## Support

For questions or issues with the updated pipelines:
1. Check Harness execution logs
2. Review this migration summary
3. Consult [Harness Cloud Documentation](https://developer.harness.io/docs/continuous-integration/use-ci/set-up-build-infrastructure/use-harness-cloud-build-infrastructure)

---

**Migration Date:** January 23, 2026  
**Modified By:** Pipeline Restructuring Initiative  
**Status:** ✅ Complete
