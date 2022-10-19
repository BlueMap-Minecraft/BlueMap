package de.bluecolored.bluemap.core.util;

import java.util.Comparator;

@FunctionalInterface
public interface IntComparator extends Comparator<Integer> {

    int compare(int o1, int o2);

    @Override
    default int compare(Integer o1, Integer o2) {
        return compare(o1.intValue(), o2.intValue());
    }

}
