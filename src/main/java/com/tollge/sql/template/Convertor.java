package com.tollge.sql.template;

import com.tollge.sql.SqlEngineException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Convertor {

    private static final BaseKey[] COLLECT_TEXT_PATTERN = new BaseKey[]{BaseKey.TEXT, BaseKey.BLANK, BaseKey.TEXT};

    public static void replaceSubTemp(List<Grammar> grammars, Map<String, List<Grammar>> subMapper) {
        for (int i = grammars.size() - 1; i >= 0; i--) {
            Grammar current = grammars.get(i);
            if (current instanceof Grammar.SubGrammar) {
                String key = ((Grammar.SubGrammar) current).getKey();
                if (subMapper.containsKey(key)) {
                    Grammar.Grammars gs = new Grammar.Grammars();
                    Grammar[] ab = subMapper.get(key).toArray(new Grammar[]{});
                    gs.put(ab);
                    grammars.set(i, gs);
                }
            } else if (current instanceof Grammar.Grammars) {
                replaceSubTemp(((Grammar.Grammars) current).getGrammars(), subMapper);
            } else if (current instanceof Grammar.If) {
                if(((Grammar.If)current).getIfTrue() instanceof Grammar.Grammars) {
                    replaceSubTemp(((Grammar.Grammars)((Grammar.If)current).getIfTrue()).getGrammars(), subMapper);
                }
                else if(((Grammar.If)current).getIfTrue() instanceof Grammar.SubGrammar) {
                    String key = ((Grammar.SubGrammar)((Grammar.If)current).getIfTrue()).getKey();
                    if (subMapper.containsKey(key)) {
                        Grammar.Grammars gs = new Grammar.Grammars();
                        Grammar[] ab = subMapper.get(key).toArray(new Grammar[]{});
                        gs.put(ab);
                        ((Grammar.If) current).setIfTrue(gs);
                    }
                }
            } else if (current instanceof Grammar.IfElse) {
                if(((Grammar.IfElse)current).getIfTrue() instanceof Grammar.Grammars) {
                    replaceSubTemp(((Grammar.Grammars) ((Grammar.IfElse) current).getIfTrue()).getGrammars(), subMapper);
                } else if(((Grammar.IfElse)current).getIfTrue() instanceof Grammar.SubGrammar) {
                    String key = ((Grammar.SubGrammar)((Grammar.IfElse)current).getIfTrue()).getKey();
                    if (subMapper.containsKey(key)) {
                        Grammar.Grammars gs = new Grammar.Grammars();
                        Grammar[] ab = subMapper.get(key).toArray(new Grammar[]{});
                        gs.put(ab);
                        ((Grammar.IfElse) current).setIfTrue(gs);
                    }
                }
                if(((Grammar.IfElse)current).getIfFalse() instanceof Grammar.Grammars) {
                    replaceSubTemp(((Grammar.Grammars) ((Grammar.IfElse) current).getIfFalse()).getGrammars(), subMapper);
                } else if(((Grammar.IfElse)current).getIfFalse() instanceof Grammar.SubGrammar) {
                    String key = ((Grammar.SubGrammar)((Grammar.IfElse)current).getIfFalse()).getKey();
                    if (subMapper.containsKey(key)) {
                        Grammar.Grammars gs = new Grammar.Grammars();
                        Grammar[] ab = subMapper.get(key).toArray(new Grammar[]{});
                        gs.put(ab);
                        ((Grammar.IfElse) current).setIfFalse(gs);
                    }
                }
            } else if (current instanceof Grammar.For) {
                if(((Grammar.For)current).getForGrammar() instanceof Grammar.Grammars) {
                    replaceSubTemp(((Grammar.Grammars)((Grammar.For)current).getForGrammar()).getGrammars(), subMapper);
                }
                else if(((Grammar.For)current).getForGrammar() instanceof Grammar.SubGrammar) {
                    String key = ((Grammar.SubGrammar)((Grammar.For)current).getForGrammar()).getKey();
                    if (subMapper.containsKey(key)) {
                        Grammar.Grammars gs = new Grammar.Grammars();
                        Grammar[] ab = subMapper.get(key).toArray(new Grammar[]{});
                        gs.put(ab);
                        ((Grammar.For) current).setForGrammar(gs);
                    }
                }
            } else if (current instanceof Grammar.ForWith) {
                if(((Grammar.ForWith)current).getForGrammar() instanceof Grammar.Grammars) {
                    replaceSubTemp(((Grammar.Grammars)((Grammar.ForWith)current).getForGrammar()).getGrammars(), subMapper);
                }
                else if(((Grammar.ForWith)current).getForGrammar() instanceof Grammar.SubGrammar){
                    String key = ((Grammar.SubGrammar)((Grammar.ForWith)current).getForGrammar()).getKey();
                    if (subMapper.containsKey(key)) {
                        Grammar.Grammars gs = new Grammar.Grammars();
                        Grammar[] ab = subMapper.get(key).toArray(new Grammar[]{});
                        gs.put(ab);
                        ((Grammar.ForWith) current).setForGrammar(gs);
                    }
                }
            }
        }
    }

    public static List<Grammar> convertGrammar(List<BaseKeyValue> baseKeys) {
        List<Object> temp = new ArrayList<>(baseKeys);

        convert(temp);

        // 全部转化成Grammar
        return temp.stream().map(o -> (Grammar) o).collect(Collectors.toList());
    }

    private static void convert(List<Object> temp) {
        boolean hasReplace = false;
        // filter 所有Grammar子类
        for (Class<?> g : Grammar.class.getDeclaredClasses()) {
            try {
                Field field = g.getDeclaredField("BASE_KEYS");
                List a = (List) field.get(null);
                if (a.isEmpty()) {
                    continue;
                }
                List<Integer> tempFinds = Sunday.find(temp.toArray(), a.toArray());

                // #a#txt#b#在匹配时会把之前的也算上, 这里会有重复, 特殊处理一下
                if (g.equals(Grammar.Property.class)) {
                    for (int i = tempFinds.size() - 2; i > 2; i -= 2) {
                        if (tempFinds.get(i).intValue() == tempFinds.get(i - 1)) {
                            tempFinds.remove(i - 1);
                            tempFinds.remove(i - 2);
                            i -= 2;
                        }
                    }
                }

                if (tempFinds != null && !tempFinds.isEmpty()) {
                    for (int i = tempFinds.size() - 1; i > 0; i -= 2) {
                        // 创建Grammar
                        Grammar grammar = (Grammar) g.newInstance();

                        // 赋值
                        int begin = tempFinds.get(i - 1);
                        int end = tempFinds.get(i);
                        grammar.putValues(temp, begin, end);

                        // 替换老对象
                        for (int j = end; j > begin; j--) {
                            temp.remove(j);
                        }
                        temp.set(begin, grammar);
                        hasReplace = true;
                    }
                }
            } catch (IllegalAccessException | NoSuchFieldException | InstantiationException e) {
                throw new SqlEngineException(e.getMessage(), e);
            }
        }

        // 合并Text和Grammar
        for (int i = temp.size() - 1; i > 0; i--) {
            if (temp.get(i).equals(BaseKey.MIX)) {
                if (temp.get(i - 1).equals(BaseKey.MIX)) {
                    hasReplace = true;
                    Grammar g1 = generateGrammar(temp.get(i - 1));
                    Grammar g2 = generateGrammar(temp.get(i));
                    Grammar.Grammars gs = new Grammar.Grammars();
                    gs.put(g1, g2);
                    temp.set(i - 1, gs);
                    temp.remove(i);
                } else if (i > 1 && temp.get(i - 1) instanceof BaseKeyValue && ((BaseKeyValue) temp.get(i - 1)).getBaseKey() == BaseKey.BLANK
                        && temp.get(i - 2).equals(BaseKey.MIX)) {
                    hasReplace = true;
                    Grammar.Text blank = new Grammar.Text();
                    blank.put(" ");
                    Grammar g1 = generateGrammar(temp.get(i - 2));
                    Grammar g2 = generateGrammar(temp.get(i));
                    Grammar.Grammars gs = new Grammar.Grammars();
                    gs.put(g1, blank, g2);
                    temp.set(i - 2, gs);
                    temp.remove(i);
                    temp.remove(--i);
                }
            }
        }

        if (hasReplace) {
            // 递归
            convert(temp);
        } else {
            // 全部替换成Grammar
            for (int i = 0; i < temp.size(); i++) {
                if (temp.get(i) instanceof BaseKeyValue) {
                    Grammar.Text t = new Grammar.Text();
                    t.put(((BaseKeyValue) temp.get(i)).getValue());
                    temp.set(i, t);
                }
            }
        }
    }

    private static Grammar generateGrammar(Object o) {
        if (o instanceof BaseKeyValue && ((BaseKeyValue) o).getBaseKey() == BaseKey.TEXT) {
            Grammar.Text t = new Grammar.Text();
            t.put(((BaseKeyValue) o).getValue());
            return t;
        } else if (o.getClass().getSuperclass().equals(Grammar.class)) {
            return (Grammar) o;
        }
        throw new SqlEngineException("generateGrammar error:" + o);
    }

    public static List<BaseKeyValue> convertBaseKey(String input) {
        // 防止括号产生的误匹配, 字符粘连导致后续计算失误
        input = input.replace("(", "( ");
        input = input.replace(")", " )");
        input = input.replaceAll("\\s+", BaseKey.BLANK.key());

        List<BaseKeyValue> result = new ArrayList<>();
        List<Integer> indexs = new ArrayList<>();
        List<BaseKey> indexBaseKey = new ArrayList<>();

        // 1.筛选出BaseKey符合的position
        for (BaseKey baseKey : BaseKey.values()) {
            if (baseKey.key().isEmpty()) {
                continue;
            }
            List<Integer> tempFinds = Sunday.find(input.toCharArray(), baseKey.key().toCharArray());
            if (tempFinds.size() % 2 != 0) {
                continue;
            }
            for (int i = 0; i < tempFinds.size(); i += 2) {
                indexs.add(tempFinds.get(i));
                indexs.add(tempFinds.get(i + 1));
                indexBaseKey.add(baseKey);
            }
        }

        // 2.排序
        QuickSort.sort(indexs, indexBaseKey, 0, indexs.size() - 2);

        // 3.组装result
        int cursor = 0;
        if (indexBaseKey.isEmpty()) {
            BaseKeyValue bv = new BaseKeyValue();
            bv.setBaseKey(BaseKey.TEXT);
            bv.setValue(input);
            result.add(bv);
        }
        for (int i = 0; i < indexBaseKey.size(); i++) {
            int left = indexs.get(i * 2);
            BaseKeyValue bv = new BaseKeyValue();
            if (cursor < left) {
                bv.setBaseKey(BaseKey.TEXT);
                bv.setValue(input.substring(cursor, left));
                cursor = left;
                i--;
            } else {
                int right = indexs.get(i * 2 + 1);
                cursor = right + 1;
                bv.setBaseKey(indexBaseKey.get(i));
                bv.setValue(input.substring(left, right + 1));
            }
            result.add(bv);
        }

        if (cursor < input.length()) {
            BaseKeyValue bv = new BaseKeyValue();
            bv.setBaseKey(BaseKey.TEXT);
            bv.setValue(input.substring(cursor, input.length()));
            result.add(bv);
        }

        // 4.语法意境推测,还原被误转化
        for (int i = result.size() - 2; i > 0; i--) {
            BaseKeyValue bv = result.get(i);
            BaseKeyValue prebv = result.get(i - 1);
            BaseKeyValue aftbv = result.get(i + 1);
            //只有2种情况, :xxx_  _xxx:
            for (BaseKey b : BaseKey.ALPHABET) {
                if (b.equals(bv.getBaseKey())) {
                    if (!(BaseKey.MARK == prebv.getBaseKey() && BaseKey.BLANK == aftbv.getBaseKey())
                            && !(BaseKey.MARK == aftbv.getBaseKey() && BaseKey.BLANK == prebv.getBaseKey())) {
                        // 转化成text
                        result.get(i).setBaseKey(BaseKey.TEXT);
                        // 合并
                        if (BaseKey.TEXT == aftbv.getBaseKey()) {
                            bv.setValue(bv.getValue().concat(aftbv.getValue()));
                            result.remove(i + 1);
                        }
                        if (BaseKey.TEXT == prebv.getBaseKey()) {
                            prebv.setValue(prebv.getValue().concat(bv.getValue()));
                            result.remove(i);
                            i--;
                        }
                    }
                }
            }

            // 去除::_或者_::中的blank, 减少匹配量
            if (bv.getBaseKey() == BaseKey.MARK) {
                if (BaseKey.MARK == prebv.getBaseKey() && BaseKey.BLANK == aftbv.getBaseKey()) {
                    result.remove(i + 1);
                }
                if (BaseKey.MARK == aftbv.getBaseKey() && BaseKey.BLANK == prebv.getBaseKey()) {
                    result.remove(i - 1);
                    i--;
                }
            }
        }

        // 5.合并text和blank
        List<Integer> textFinds = Sunday.find(result.toArray(), COLLECT_TEXT_PATTERN);

        // 5.1合并相邻的position
        for (int i = textFinds.size() - 1; i > 0; i--) {
            if (textFinds.get(i).equals(textFinds.get(i - 1))) {
                textFinds.remove(i);
                textFinds.remove(i - 1);
                i--;
            }
        }

        // 5.2合并筛选结果
        for (int i = textFinds.size() - 1; i > 0; i -= 2) {
            int begin = textFinds.get(i - 1);
            int end = textFinds.get(i);
            for (int j = end; j > begin; j--) {
                result.set(j - 1, collectBaseKeyValue(result.get(j - 1), result.get(j)));
                result.remove(j);
            }
        }

        return result;
    }

    private static BaseKeyValue collectBaseKeyValue(BaseKeyValue baseKeyValue, BaseKeyValue baseKeyValue1) {
        baseKeyValue.setBaseKey(BaseKey.TEXT);
        baseKeyValue.setValue(baseKeyValue.getValue().concat(baseKeyValue1.getValue()));
        return baseKeyValue;
    }
}
