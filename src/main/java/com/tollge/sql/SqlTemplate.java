package com.tollge.sql;

import com.tollge.sql.file.FileKeyword;
import com.tollge.sql.template.BaseKeyValue;
import com.tollge.sql.template.Convertor;
import com.tollge.sql.template.Grammar;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * sql模板
 *
 * @author toyer
 */
public class SqlTemplate {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(SqlTemplate.class);
    private SqlTemplate() {
    }

    private static final String DEFAULT_FILE_TYPE = ".md";
    private static Map<String, List<Grammar>> mapper = new HashMap<>();

    static {
        try {
            Map<String, List<Grammar>> subMapper = new HashMap<>();

            String path = "mapper";
            try (InputStream pathIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(pathIS))) {
                for (String filename : br.lines().collect(Collectors.toList())) {
                    if (!filename.endsWith(DEFAULT_FILE_TYPE)) {
                        continue;
                    }

                    try(InputStream fileIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(path + "/" + filename);
                        BufferedReader brFile = new BufferedReader(new InputStreamReader(fileIS))) {

                        String fileName = filename.substring(0, filename.length() - DEFAULT_FILE_TYPE.length());
                        String line;

                        StringBuilder contextBuilder = null;
                        boolean subBegin = false;
                        String name = "";
                        while ((line = brFile.readLine()) != null) {
                            if(contextBuilder != null && !line.startsWith(FileKeyword.END)) {
                                contextBuilder.append(line).append("\n");
                                continue;
                            }

                            if(line.startsWith(FileKeyword.NAME_PREFIX)) {
                                name = line.substring(FileKeyword.NAME_PREFIX.length());
                            } else if (line.startsWith(FileKeyword.SQL_BEGIN)) {
                                contextBuilder = new StringBuilder();
                            } else if (line.startsWith(FileKeyword.SUB_BEGIN)) {
                                contextBuilder = new StringBuilder();
                                subBegin = true;
                            } else if (line.startsWith(FileKeyword.END)) {
                                if("".equals(name) || contextBuilder == null) {
                                    throw new SqlEngineException("解析错误:"+path+"/"+filename+":"+line);
                                }

                                String key = fileName.concat(".").concat(name);
                                log.debug("SQL template load " + key);

                                contextBuilder.deleteCharAt(contextBuilder.length() - 1);

                                if (subBegin) {
                                    subMapper.put(key, SqlTemplate.generateGrammars(contextBuilder.toString(), null));
                                } else {
                                    mapper.put(key, SqlTemplate.generateGrammars(contextBuilder.toString(), subMapper));
                                }

                                name = "";
                                contextBuilder = null;
                                subBegin = false;
                            }

                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new SqlEngineException("init sql failed", e);
        }
    }

    private static List<Grammar> generateGrammars(String sql, Map<String, List<Grammar>> subMapper) {
        // 基础转化
        List<BaseKeyValue> baseKeys = Convertor.convertBaseKey(sql);

        // 语法适配
        List<Grammar> grammars = Convertor.convertGrammar(baseKeys);

        // 替换掉subGrammar
        if (subMapper != null) {
            Convertor.replaceSubTemp(grammars, subMapper);
        }

        return grammars;
    }


    public static SqlSession generateSQL(String key, Map<String, Object> inputs) {
        StringBuilder sqlSb = new StringBuilder();
        List<Object> params = new ArrayList<>();

        List<Grammar> grammars = mapper.get(key);

        for (Grammar grammar : grammars) {
            grammar.string(sqlSb, params, inputs);
        }

        SqlSession sqlSession = new SqlSession(sqlSb.toString());
        sqlSession.setParams(params);

        if (log.isDebugEnabled()) {
            log.debug(sqlSession.toString());
        }

        return sqlSession;
    }
}

