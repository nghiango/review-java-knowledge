# CI/CD Hands-On Exercises

Practical engineering exercises to master zero-downtime database migrations, automated rollback deployment pipelines, and architectural fitness enforcement in CI.

---

## Exercise 1: Refactoring a Relational Table with the Expand-Contract Pattern

### Objective
Decompose a database refactoring task into backward-compatible Flyway migrations so the schema evolves across multiple releases without downtime.

### Scenario
An existing `orders` table stores payment status as an integer code (`status_code INT`: 1=PENDING, 2=PAID, 3=CANCELLED). The engineering team needs to migrate to an enumerated string column (`payment_status VARCHAR(20)`: 'PENDING', 'PAID', 'CANCELLED').

### Step-by-Step Implementation

??? question "View solution"
    #### Step 1: Expand Phase (`V2__expand_payment_status.sql`)
    Add the new column as nullable and attach a trigger to synchronize writes from $v1$:
    ```sql
    ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20);

    CREATE OR REPLACE FUNCTION sync_order_status()
    RETURNS TRIGGER AS $$
    BEGIN
        IF NEW.payment_status IS NULL AND NEW.status_code IS NOT NULL THEN
            NEW.payment_status := CASE NEW.status_code
                WHEN 1 THEN 'PENDING'
                WHEN 2 THEN 'PAID'
                WHEN 3 THEN 'CANCELLED'
                ELSE 'UNKNOWN'
            END;
        END IF;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;

    DROP TRIGGER IF EXISTS trg_sync_order_status ON orders;
    CREATE TRIGGER trg_sync_order_status
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION sync_order_status();
    ```

    #### Step 2: Backfill Historical Data (Asynchronous Batch Script)
    Run in small chunked transactions outside peak traffic:
    ```sql
    UPDATE orders
    SET payment_status = CASE status_code
        WHEN 1 THEN 'PENDING'
        WHEN 2 THEN 'PAID'
        WHEN 3 THEN 'CANCELLED'
        ELSE 'UNKNOWN'
    END
    WHERE payment_status IS NULL AND id BETWEEN 1 AND 50000;
    ```

    #### Step 3: Transition Phase (Deploy Application $v2$)
    Deploy the new application version reading and writing strictly to `payment_status`.

    #### Step 4: Contract Phase (`V4__contract_drop_legacy_status.sql`)
    Deployed weeks later after $v2$ has proven stable:
    ```sql
    DROP TRIGGER IF EXISTS trg_sync_order_status ON orders;
    DROP FUNCTION IF EXISTS sync_order_status();
    ALTER TABLE orders DROP COLUMN IF EXISTS status_code;
    ```

---

## Exercise 2: Implementing Automated Rollbacks in GitHub Actions

### Objective
Write a GitHub Actions deployment step that verifies application readiness and automatically triggers a rollback if the new container revision fails health probes.

??? question "View solution"
    ```yaml
    - name: Deploy and Verify with Automated Rollback
      env:
        CLUSTER: production-cluster
        SERVICE: order-service
      run: |
        # 1. Capture currently active task definition ARN
        PREV_TASK_DEF=$(aws ecs describe-services \
          --cluster "$CLUSTER" \
          --services "$SERVICE" \
          --query 'services[0].taskDefinition' \
          --output text)
        echo "Active task definition before deployment: $PREV_TASK_DEF"

        # 2. Trigger deployment with new task definition revision
        aws ecs update-service \
          --cluster "$CLUSTER" \
          --service "$SERVICE" \
          --task-definition "${{ steps.register_task.outputs.task_def_arn }}"

        # 3. Wait for service stability
        if ! aws ecs wait services-stable --cluster "$CLUSTER" --services "$SERVICE"; then
          echo "Deployment failed to stabilize! Triggering automated rollback..."
          aws ecs update-service \
            --cluster "$CLUSTER" \
            --service "$SERVICE" \
            --task-definition "$PREV_TASK_DEF"
          exit 1
        fi

        # 4. Execute synthetic smoke test
        if ! curl -sf https://api.internal/actuator/health/readiness; then
          echo "Synthetic smoke test failed! Triggering automated rollback..."
          aws ecs update-service \
            --cluster "$CLUSTER" \
            --service "$SERVICE" \
            --task-definition "$PREV_TASK_DEF"
          exit 1
        fi

        echo "Deployment succeeded and passed all verification gates!"
    ```

---

## Exercise 3: Writing an ArchUnit Test to Ban Field Injection in CI

### Objective
Write a Java ArchUnit fitness function that runs during `./gradlew test` and fails the build if any class uses `@Autowired` on private fields instead of constructor injection.

??? question "View solution"
    ```java
    package lab.architecture;

    import com.tngtech.archunit.core.importer.ImportOption;
    import com.tngtech.archunit.junit.AnalyzeClasses;
    import com.tngtech.archunit.junit.ArchTest;
    import com.tngtech.archunit.lang.ArchRule;
    import org.springframework.beans.factory.annotation.Autowired;

    import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

    @AnalyzeClasses(packages = "lab", importOptions = ImportOption.DoNotIncludeTests.class)
    public class DependencyInjectionRuleTest {

        @ArchTest
        public static final ArchRule no_field_injection =
            noFields()
                .should().beAnnotatedWith(Autowired.class)
                .because("Field injection is prohibited; always use constructor injection for explicit dependencies and immutability.");
    }
    ```
