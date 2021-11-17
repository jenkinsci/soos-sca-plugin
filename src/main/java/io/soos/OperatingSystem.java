package io.soos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperatingSystem {

    LINUX("Linux","unix"),
    MAC("Mac OS","mac"),
    WINDOWS("Windows","win");

    private String name;
    private String value;

}
