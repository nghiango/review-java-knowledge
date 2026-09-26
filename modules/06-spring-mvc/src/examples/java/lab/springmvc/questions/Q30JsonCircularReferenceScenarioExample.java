package lab.springmvc.questions;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q30JsonCircularReferenceScenarioExample {
    private Q30JsonCircularReferenceScenarioExample() {}

    // Parent entity with managed reference to child list
    public static class Department {
        private String name;

        @JsonManagedReference private List<Employee> employees = new ArrayList<>();

        public Department(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<Employee> getEmployees() {
            return employees;
        }
    }

    // Child entity with back-reference pointing back to parent:
    // Without @JsonBackReference, Jackson attempts to serialize department -> employees ->
    // department -> ...
    // resulting in infinite recursion and a 500 Internal Server Error (or StackOverflowError)!
    public static class Employee {
        private String name;

        @JsonBackReference private Department department;

        public Employee(String name, Department department) {
            this.name = name;
            this.department = department;
        }

        public String getName() {
            return name;
        }

        public Department getDepartment() {
            return department;
        }
    }

    public static void main(String[] args) throws Exception {
        Department dept = new Department("Engineering");
        Employee emp = new Employee("Alice", dept);
        dept.getEmployees().add(emp);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(dept);

        // Circular loop is broken: department contains employee, but employee's department field is
        // omitted:
        boolean serialized = json.contains("Engineering") && json.contains("Alice"); // true
    }
}
