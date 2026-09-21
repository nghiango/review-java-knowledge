package lab.java25boot4.springmvc.questions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Q02JSpecifyNullMarkedBasicsExample {

    public record UserDto(String id, @Nullable String nickname) {}

    public static void main(String[] args) {
        var user = new UserDto("U100", null);
        System.out.println(user.id()); // U100
        System.out.println(user.nickname()); // null
    }
}
