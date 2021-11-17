package io.soos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OnFailure {

    FAIL_THE_BUILD("Fail the build","fail_the_build"),
    CONTINUE_ON_FAILURE("Continue the build","continue_on_failure");

    private String name;
    private String value;

}
