package com.wind.common.query.cursor;

import com.wind.common.WindConstants;

import java.util.Arrays;

/**
 * @author wuxp
 * @date 2025-10-17 17:26
 **/
final class WindArrayStringUtils {

    private WindArrayStringUtils() {
        throw new AssertionError();
    }


    static String arrayToString(Object o) {
        if (o instanceof byte[] v) {
            return byteToString(v);
        }
        if (o instanceof short[] v) {
            return shortToString(v);
        }
        if (o instanceof int[] v) {
            return intToString(v);
        }
        if (o instanceof long[] v) {
            return longToString(v);
        }
        if (o instanceof double[] v) {
            return doubleToString(v);
        }
        if (o instanceof float[] v) {
            return floatToString(v);
        }
        if (o instanceof boolean[] v) {
            return booleanToString(v);
        }
        return String.join(WindConstants.COMMA, Arrays.stream((Object[]) o).map(Object::toString).toList());
    }

    private static String byteToString(byte[] val) {
        StringBuilder result = new StringBuilder();
        for (byte b : val) {
            result.append(b);
            result.append(WindConstants.COMMA);
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String shortToString(short[] val) {
        StringBuilder result = new StringBuilder();
        for (short b : val) {
            result.append(b);
            result.append(WindConstants.COMMA);
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String intToString(int[] val) {
        StringBuilder result = new StringBuilder();
        for (int b : val) {
            result.append(b);
            result.append(WindConstants.COMMA);
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String longToString(long[] val) {
        StringBuilder result = new StringBuilder();
        for (long b : val) {
            result.append(b);
            result.append(WindConstants.COMMA);
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String floatToString(float[] val) {
        StringBuilder result = new StringBuilder();
        for (float b : val) {
            result.append(b);
            result.append(WindConstants.COMMA);
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String doubleToString(double[] val) {
        StringBuilder result = new StringBuilder();
        for (double b : val) {
            result.append(b);
            result.append(WindConstants.COMMA);
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String booleanToString(boolean[] val) {
        StringBuilder result = new StringBuilder();
        for (boolean b : val) {
            result.append(b);
            result.append(WindConstants.COMMA);
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }


}
