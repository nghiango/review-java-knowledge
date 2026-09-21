package lab.architecture.questions;

import java.util.Set;

/**
 * Q15: Spring Modulith Module Verification & Event Publication. Demonstrates the concept of
 * declared module boundaries and allowed inter-module dependencies.
 */
public class Q15SpringModulithVerificationExample {

    public static class ModuleDescriptor {
        private final String moduleName;
        private final Set<String> exposedPackages;
        private final Set<String> allowedDependencies;

        public ModuleDescriptor(
                String moduleName, Set<String> exposedPackages, Set<String> allowedDependencies) {
            this.moduleName = moduleName;
            this.exposedPackages = exposedPackages;
            this.allowedDependencies = allowedDependencies;
        }

        public boolean canDependOn(String targetModule, String targetPackage) {
            if (!allowedDependencies.contains(targetModule)) {
                return false;
            }
            return targetPackage.startsWith("lab." + targetModule + ".api");
        }
    }

    public static void main(String[] args) {
        ModuleDescriptor orderModule =
                new ModuleDescriptor("ordering", Set.of("lab.ordering.api"), Set.of("inventory"));

        boolean allowedPublicAccess =
                orderModule.canDependOn("inventory", "lab.inventory.api.InventoryService"); // true
        boolean forbiddenInternalAccess =
                orderModule.canDependOn(
                        "inventory", "lab.inventory.internal.Repo"); // false (violates boundary)

        System.out.println(
                "Q15 allowed: " + allowedPublicAccess + ", forbidden: " + forbiddenInternalAccess);
    }
}
