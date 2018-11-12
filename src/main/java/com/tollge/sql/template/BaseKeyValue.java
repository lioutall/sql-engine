package com.tollge.sql.template;

/**
 * BaseKey 带参数
 *
 * @author toyer
 * @since 2018-03-08
 */
public class BaseKeyValue {
    private BaseKey baseKey;

    private String value;

    @Override
    public boolean equals(Object o) {
        if(o instanceof BaseKeyValue) {
            return this.baseKey == ((BaseKeyValue) o).getBaseKey();
        }
        if(o instanceof BaseKey) {
            if(BaseKey.MIX.equals(o)) {
                return this.baseKey.equals(BaseKey.TEXT);
            }
            return this.baseKey.equals(o);
        }
        return false;
    }

    public BaseKey getBaseKey() {
        return baseKey;
    }

    public void setBaseKey(BaseKey baseKey) {
        this.baseKey = baseKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
