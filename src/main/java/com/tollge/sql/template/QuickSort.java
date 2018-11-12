package com.tollge.sql.template;

import java.util.List;

/**
 * 快速排序 - 定制实现
 *
 * @author toyer
 * @since 2018-03-08
 */
public class QuickSort {

    /**
     * 快速排序的主函数(定制款两位快排)
     * indexBaseKey如果传null,则是针对a的快排
     * @param a []
     * @param indexBaseKey []
     * @param s []
     * @param e []
     */
    public static void sort(List<Integer> a, List<BaseKey> indexBaseKey, int s, int e) {

        int quickMove;
        if (s < e) {
            quickMove = partition(a,indexBaseKey, s, e);
            sort(a, indexBaseKey, s, quickMove - 2);
            sort(a, indexBaseKey, quickMove + 2, e);
        }
    }

    // 序列的划分  使用最后固定的一项
    private static int partition(List<Integer> a, List<BaseKey> indexBaseKey, int s, int e) {
        Integer temp = a.get(e);
        int i = s - 2;
        // 这个地方是不断的遍历所有的列表的前面的所有的数据项发现比参照数据小的数据项
        // 然后和最前面比参照项大的数据项进行交换
        for (int j = s; j < e; j+=2) {
            if (a.get(j).compareTo(temp) < 0) {
                i = i + 2;
                swap(a, indexBaseKey, i, j);
            }
        }

        swap(a, indexBaseKey, i + 2, e);
        return i + 2;
    }

    private static void swap(List<Integer> a, List<BaseKey> indexBaseKey, int i, int j) {
        a.set(i, a.set(j, a.get(i)));
        a.set(i+1, a.set(j+1, a.get(i+1)));
        if(indexBaseKey != null) {
            indexBaseKey.set(i / 2, indexBaseKey.set(j / 2, indexBaseKey.get(i / 2)));
        }
    }

}
