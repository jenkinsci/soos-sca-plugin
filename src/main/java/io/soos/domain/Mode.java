package io.soos.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mode {
    RUN_AND_WAIT("Run and wait","run_and_wait"),
    ASYNC_INIT("Async init","async_init"),
    ASYNC_RESULT("Async result","async_result");

    private String name;
    private String value;

}
