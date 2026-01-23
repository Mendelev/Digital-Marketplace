# Pipeline Quick Reference

## Updated Pipeline Structure

All service pipelines now follow this simplified structure:

### Runtime Configuration
```yaml
runtime:
  type: Cloud  # Changed from Docker
  spec: {}
```

### Build Step
```yaml
- step:
    type: Run
    name: Build_Service
    identifier: Build_Service
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

## Key Changes

| Aspect | Before | After |
|--------|--------|-------|
| **Runtime** | Docker | Harness Cloud |
| **Image Connector** | account.Dockerhub | account.harnessImage |
| **Build Steps** | 2 templates | 1 inline step |
| **Deployment** | JFrog Artifactory | None (build only) |
| **Variables** | 5 variables | 2 variables |
| **Secrets** | jfrog_username, jfrog_token | None (except Snyk) |

## Service Pipelines

| Service | Pipeline File | Special Features |
|---------|--------------|------------------|
| Auth Service | `auth_service_build.yaml` | Standard build |
| User Service | `user_service_build.yaml` | Standard build |
| Catalog Service | `catalog_service_build.yaml` | Standard build |
| Cart Service | `cart_service_build.yaml` | Standard build |
| Order Service | `order_service_build.yaml` | Standard build |
| Payment Service | `payment_service_build.yaml` | Standard build |
| Inventory Service | `inventory_service_build.yaml` | Standard build |
| Shipping Service | `shipping_service_build.yaml` | **+ Snyk Security Scan** |
| Search Service | `search_service_build.yaml` | Standard build |
| Shared DTOs | `shared_dtos_build.yaml` | Module build |

## Variables Per Pipeline

### Standard Services
```yaml
variables:
  - name: serviceName
    type: String
    description: Service directory name
    required: true
    value: <service-name>  # e.g., auth-service
  - name: artifactId
    type: String
    description: Maven artifactId from pom.xml
    required: true
    value: <service-name>  # e.g., auth-service
```

### Shared DTOs
```yaml
variables:
  - name: modulePath
    type: String
    value: common/shared-dtos
  - name: artifactId
    type: String
    value: shared-dtos
  - name: stableVersion
    type: String
    value: 1.0.0
```

## Execution Examples

### Trigger a Build
```bash
# Via Harness UI:
# 1. Navigate to Pipeline
# 2. Click "Run"
# 3. Select branch (usually "main")
# 4. Click "Run Pipeline"

# Expected output:
# ✅ Cloned repository
# ✅ Built service
# ✅ Generated JAR: target/service-name-1.0.0-SNAPSHOT.jar
```

### Shipping Service (with Snyk)
```bash
# Additional step after build:
# ✅ Snyk security scan
# ✅ Vulnerability report
# ✅ Security findings in Harness UI
```

## Build Times (Approximate)

| Service | Estimated Time | Notes |
|---------|---------------|-------|
| Auth Service | 2-3 min | Standard build |
| User Service | 2-3 min | Standard build |
| Catalog Service | 2-4 min | Larger codebase |
| Order Service | 2-4 min | Larger codebase |
| Payment Service | 2-3 min | Standard build |
| Inventory Service | 2-3 min | Standard build |
| Shipping Service | 4-6 min | **Includes Snyk scan** |
| Search Service | 2-3 min | Standard build |
| Shared DTOs | 1-2 min | Small module |

## Common Issues & Solutions

### Issue: "connectorRef not found"
**Solution:** Ensure `account.harnessImage` connector exists in Harness

### Issue: "Service directory not found"
**Solution:** Verify `serviceName` matches actual directory name in repository

### Issue: "Maven build failed"
**Solution:** Check service's `pom.xml` for errors, run locally first

### Issue: "Snyk token invalid"
**Solution:** Update `snyk_access_token` secret in Harness

## Next Steps

1. **Test each pipeline** - Run at least once to verify
2. **Monitor builds** - Check execution times and success rates
3. **Clean up secrets** - Remove unused JFrog secrets
4. **Update documentation** - Inform team of new process
5. **Consider enhancements** - Add Docker builds, deploy steps, etc.

## Resources

- [MIGRATION_SUMMARY.md](MIGRATION_SUMMARY.md) - Detailed migration documentation
- [Harness Cloud Docs](https://developer.harness.io/docs/continuous-integration/use-ci/set-up-build-infrastructure/use-harness-cloud-build-infrastructure)
- [Maven on Harness](https://developer.harness.io/docs/continuous-integration/use-ci/build-and-upload-artifacts/build-and-upload-an-artifact/#maven)
