package com.tollge.sql.template;

import com.tollge.sql.template.jexparser.ExParser;

import java.util.*;

/**
 * 语法
 *
 * @author toyer
 * @since 2018-03-08
 */
public abstract class Grammar {
    private static final String REPLACE = "?";
    private static ExParser parser = new ExParser();

    /**
     * Grammar与mix匹配上
     *
     * @param o []
     * @return []
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof BaseKey && BaseKey.MIX.equals(o);
    }

    public abstract void putValues(List<Object> temp, int begin, int end);

    public abstract void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs);

    protected Grammar putGrammar(Object o) {
        if (o instanceof BaseKeyValue) {
            Text t = new Text();
            t.put(((BaseKeyValue) o).getValue());
            return t;
        } else {
            return (Grammar) o;
        }
    }

    static class If extends Grammar {
        public If() {
        }

        public static final List<BaseKey> BASE_KEYS = Arrays.asList(BaseKey.MARK, BaseKey.IF, BaseKey.BLANK, BaseKey.TEXT, BaseKey.MARK, BaseKey.MARK,
                BaseKey.MIX, BaseKey.BLANK,
                BaseKey.IF, BaseKey.MARK);
        private Grammar ifTrue;

        public Grammar getIfTrue() {
            return ifTrue;
        }

        public void setIfTrue(Grammar ifTrue) {
            this.ifTrue = ifTrue;
        }

        // 表达式引擎, 使用了开源的jexparser
        /**
         * 支持以下表达式
         * >  <  =  >=  <=
         * +  -  *  /
         * or  and  not
         * '  ( )  ?  0~9 .
         * equals  equalsIgnoreCase  contains  containsIgnoreCase
         * startWith  startWithIgnoreCase  endWith  endWithIgnoreCase
         */
        private ExParser.ExpItem[] expItems;

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
            String condition = ((BaseKeyValue) temp.get(begin + 3)).getValue();
            expItems = parser.compile(condition);
            ifTrue = putGrammar(temp.get(begin + 6));
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
            if ((Boolean) parser.doParse(expItems, inputs)) {
                ifTrue.string(sb, params, inputs);
            }
        }
    }


    static class IfElse extends Grammar {
        public IfElse() {
        }

        public static final List<BaseKey> BASE_KEYS = Arrays.asList(BaseKey.MARK, BaseKey.IF, BaseKey.BLANK, BaseKey.TEXT, BaseKey.MARK, BaseKey.MARK,
                BaseKey.MIX, BaseKey.BLANK,
                BaseKey.MARK, BaseKey.ELSE, BaseKey.BLANK,
                BaseKey.MIX, BaseKey.BLANK,
                BaseKey.IF, BaseKey.MARK);

        // 表达式引擎, 使用了开源的jexparser
        private ExParser.ExpItem[] expItems;

        private Grammar ifTrue;
        private Grammar ifFalse;

        public Grammar getIfTrue() {
            return ifTrue;
        }

        public void setIfTrue(Grammar ifTrue) {
            this.ifTrue = ifTrue;
        }

        public Grammar getIfFalse() {
            return ifFalse;
        }

        public void setIfFalse(Grammar ifFalse) {
            this.ifFalse = ifFalse;
        }

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
            String condition = ((BaseKeyValue) temp.get(begin + 3)).getValue();
            expItems = parser.compile(condition);
            ifTrue = putGrammar(temp.get(begin + 6));
            ifFalse = putGrammar(temp.get(begin + 11));
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
            if ((Boolean) parser.doParse(expItems, inputs)) {
                ifTrue.string(sb, params, inputs);
            } else {
                ifFalse.string(sb, params, inputs);
            }
        }
    }

    static class For extends Grammar {
        public For() {
        }

        public static final List<BaseKey> BASE_KEYS = Arrays.asList(BaseKey.MARK, BaseKey.FOR, BaseKey.BLANK, BaseKey.TEXT, BaseKey.MARK, BaseKey.TEXT, BaseKey.MARK, BaseKey.MARK,
                BaseKey.MIX, BaseKey.BLANK,
                BaseKey.FOR, BaseKey.MARK);

        private String item;
        private String items;
        private Grammar forGrammar;

        public Grammar getForGrammar() {
            return forGrammar;
        }
        public void setForGrammar(Grammar forGrammar) {
            this.forGrammar = forGrammar;
        }

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
            item = ((BaseKeyValue) temp.get(begin + 3)).getValue();
            items = ((BaseKeyValue) temp.get(begin + 5)).getValue();
            forGrammar = putGrammar(temp.get(begin + 8));
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
            ((Collection) inputs.get(items)).forEach(i -> {
                inputs.put(item, i);
                forGrammar.string(sb, params, inputs);
            });
        }
    }

    static class ForWith extends Grammar {
        public ForWith() {
        }

        public static final List<BaseKey> BASE_KEYS = Arrays.asList(BaseKey.MARK, BaseKey.FOR, BaseKey.BLANK, BaseKey.TEXT, BaseKey.MARK, BaseKey.TEXT, BaseKey.BLANK, BaseKey.MARK, BaseKey.WITH, BaseKey.BLANK, BaseKey.TEXT, BaseKey.MARK, BaseKey.MARK,
                BaseKey.MIX, BaseKey.BLANK,
                BaseKey.FOR, BaseKey.MARK);

        private String item;
        private String items;
        private String with;
        private Grammar forGrammar;

        public Grammar getForGrammar() {
            return forGrammar;
        }
        public void setForGrammar(Grammar forGrammar) {
            this.forGrammar = forGrammar;
        }

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
            item = ((BaseKeyValue) temp.get(begin + 3)).getValue();
            items = ((BaseKeyValue) temp.get(begin + 5)).getValue();
            with = ((BaseKeyValue) temp.get(begin + 10)).getValue();
            forGrammar = putGrammar(temp.get(begin + 13));
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
            ((Collection) inputs.get(items)).forEach(i -> {
                inputs.put(item, i);
                forGrammar.string(sb, params, inputs);
                sb.append(with);
            });
            int length = sb.length();
            for (int i = 1; i <= with.length(); i++) {
                sb.deleteCharAt(length - i);
            }
        }
    }

    static class Property extends Grammar {
        public Property() {
        }

        public static final List<BaseKey> BASE_KEYS = Arrays.asList(BaseKey.HASHTAG, BaseKey.TEXT, BaseKey.HASHTAG);

        private String key;

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
            key = ((BaseKeyValue) temp.get(begin + 1)).getValue();
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
          if ("$".equals(inputs.get("sql_engine_mode"))) {
            sb.append("$").append(params.size()+1);
            params.add(inputs.get(key));
          } else {
            sb.append(REPLACE);
            params.add(inputs.get(key));
          }
        }
    }

    static class Text extends Grammar {
        public Text() {
        }

        public static final List<BaseKey> BASE_KEYS = new ArrayList<>();
        private String key;

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
            sb.append(key);
        }

        public void put(String key) {
            this.key = key;
        }
    }

    static class Grammars extends Grammar {
        public Grammars() {
        }

        public static final List<BaseKey> BASE_KEYS = new ArrayList<>();

        public List<Grammar> getGrammars() {
            return grammars;
        }

        private LinkedList<Grammar> grammars;

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
            for (Grammar grammar : grammars) {
                grammar.string(sb, params, inputs);
            }
        }

        public void put(Grammar... g) {
            grammars = new LinkedList<>();
            grammars.addAll(Arrays.asList(g));
        }

        public void addAfter(Grammar g) {
            grammars.addLast(g);
        }

        public void addBefore(Grammar g) {
            grammars.addFirst(g);
        }
    }

    static class SubGrammar extends Grammar {
        public SubGrammar() {
        }

        public static final List<BaseKey> BASE_KEYS = Arrays.asList(BaseKey.MARK, BaseKey.SUB, BaseKey.BLANK, BaseKey.TEXT, BaseKey.BLANK, BaseKey.SUB, BaseKey.MARK);
        private String key;

        @Override
        public void putValues(List<Object> temp, int begin, int end) {
            key = ((BaseKeyValue) temp.get(begin + 3)).getValue();
        }

        @Override
        public void string(StringBuilder sb, List<Object> params, Map<String, Object> inputs) {
        }

        public String getKey() {
            return key;
        }
    }


}
