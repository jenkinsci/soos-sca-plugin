package io.soos.commons;
public interface ErrorMessage {

    public String SHOULD_NOT_BE_NULL = "Should not be null";
    public String SHOULD_BE_A_NUMBER = "Should be a number";


    static public String shouldBeMoreThanXCharacters(Integer number){
        StringBuilder msg = new StringBuilder("Should be more than ").append(number).append("characters.");
        return msg.toString();
    }
}


