package com.tollge.sql.template;

import java.util.ArrayList;
import java.util.List;

/**
 * Sunday算法
 *
 * @author toyer
 * @created 2018-03-07
 */
public class Sunday {

    public static List<Integer> find(char[] destchars, char[] patternchars) {
        List<Integer> result = new ArrayList<>();

        int i = 0;
        int j = 0;
        while (i <= (destchars.length - patternchars.length + j)) {
            if (destchars[i] != patternchars[j]) {
                if (i == (destchars.length - patternchars.length + j)) {
                    break;
                }
                int pos = contains(patternchars, destchars[i + patternchars.length - j]);
                if (pos == -1) {
                    i = i + patternchars.length + 1 - j;
                    j = 0;
                } else {
                    i = i + patternchars.length - pos - j;
                    j = 0;
                }
            } else {
                if (j == (patternchars.length - 1)) {
                    result.add(i - j);
                    result.add(i);
                    i = i - j + 1;
                    j = 0;
                } else {
                    i++;
                    j++;
                }
            }
        }

        return result;
    }

    private static int contains(char[] chars, char target) {
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == target) {
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> find(Object[] dest, Object[] pattern) {
        List<Integer> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i <= (dest.length - pattern.length + j)) {
            if (!dest[i].equals(pattern[j])) {
                if (i == (dest.length - pattern.length + j)) {
                    break;
                }
                int pos = contains(pattern, dest[i + pattern.length - j]);
                if (pos == -1) {
                    i = i + pattern.length + 1 - j;
                    j = 0;
                } else {
                    i = i + pattern.length - pos - j;
                    j = 0;
                }
            } else {
                if (j == (pattern.length - 1)) {
                    result.add(i - j);
                    result.add(i);
                    i = i - j + 1;
                    j = 0;
                } else {
                    i++;
                    j++;
                }
            }
        }
        return result;
    }

    private static int contains(Object[] chars, Object target) {
        for (int i = chars.length - 1; i >= 0; i--) {
            if (target.equals(chars[i])) {
                return i;
            }
        }
        return -1;
    }
}

