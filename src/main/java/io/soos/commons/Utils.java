package io.soos.commons;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class Utils {
    public static Boolean validateIsNumeric(String value) {
        try {
            return StringUtils.isNumeric(value);
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
}
