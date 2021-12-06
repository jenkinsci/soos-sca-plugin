package io.soos.commons;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class Utils {
    public static Boolean validateIsNotEmptyAndIsNumeric(String value) {
        return !ObjectUtils.isEmpty(value) && !StringUtils.isNumeric(value);
    }
}
