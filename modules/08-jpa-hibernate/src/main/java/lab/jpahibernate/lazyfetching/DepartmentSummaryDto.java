package lab.jpahibernate.lazyfetching;

public record DepartmentSummaryDto(
        Long departmentId, String name, int employeeCount, int projectCount) {}
