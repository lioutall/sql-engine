package com.tollge.sql;

import java.util.List;

/**
 * Bean对象
 *
 * @author toyer
 * @created 2017-12-12
 */
public class SqlSession {
    public SqlSession(String sql) {
        this.sql = sql;
    }

    private String sql;
    private List<Object> params;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return sql + "\t" + params;
    }
}
