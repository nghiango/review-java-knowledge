package lab.java25boot4.springmvc.questions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class Q04ProblemDetailApiVersionExample {

    public static void main(String[] args) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_ACCEPTABLE,
                        "Requested API version 9.0 is deprecated or unsupported");
        problem.setTitle("Unsupported API Version");
        problem.setProperty("supportedVersions", new String[] {"1.0", "2.0"});

        System.out.println(problem.getStatus()); // 406
        System.out.println(problem.getTitle()); // Unsupported API Version
        System.out.println(
                problem.getDetail()); // Requested API version 9.0 is deprecated or unsupported
    }
}
