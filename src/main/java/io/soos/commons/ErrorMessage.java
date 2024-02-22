package io.soos.commons;

public final class ErrorMessage {

    public static final String SHOULD_NOT_BE_NULL = "Should not be null";
    public static final String SHOULD_BE_A_NUMBER = "Should be a number";

    public static String shouldBeMoreThanXCharacters(Integer number) {
        StringBuilder msg = new StringBuilder("Should be more than ").append(number).append("characters.");
        return msg.toString();
    }
}


