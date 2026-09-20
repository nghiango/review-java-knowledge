package lab.performance.questions;

public final class Q19TailAmplificationExample {
    public static void main(String[] args) {
        double oneCallSuccess = 0.99;
        double allTenFast = Math.pow(oneCallSuccess, 10); // about 0.904
        System.out.println(allTenFast);
    }
}
