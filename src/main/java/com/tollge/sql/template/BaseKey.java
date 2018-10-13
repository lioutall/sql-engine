package com.tollge.sql.template;

/**
 * @author toyer
 */
public enum BaseKey {
    // 匹配关键字
    FOR("for"),
    IF("if"),
    ELSE("else"),
    HASHTAG("#"),
    MARK("?"),
    BLANK(" "),

    WITH("with"),
    SUB("sub"),

    // text和mix是无法匹配的,只能程序分配
    TEXT(""),
    MIX("")
    ;

    private final String key;

    BaseKey(String key) {
        this.key = key;
    }

    public final String key() {
        return key;
    }

    /**
     *字母集合,需要做语境推测
     */
    protected static final BaseKey[] ALPHABET = new BaseKey[] {FOR, IF, ELSE, WITH, SUB};

}
