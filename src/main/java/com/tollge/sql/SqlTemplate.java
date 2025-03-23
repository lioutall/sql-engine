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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

  public static final String TEMPLATE_DIR = "mapper";

  static {
        try {
            Map<String, List<Grammar>> subMapper = new HashMap<>();

          String path = SqlTemplate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            log.debug("找到jar包位置:{}", path);
            if(path.endsWith(".jar") && !path.contains("sql-engine")) {
              scanDirInJar(path, TEMPLATE_DIR, subMapper);
            }

          scanDirInResource(TEMPLATE_DIR, subMapper);

          // 全部做好之后, 统一替换掉subGrammar
            if (!subMapper.isEmpty()) {
                for(List<Grammar> grammars : mapper.values()){
                    Convertor.replaceSubTemp(grammars, subMapper);
                }
            }
        } catch (IOException e) {
            throw new SqlEngineException("init sql failed", e);
        }
    }

  private static void scanDirInJar(String path, String templateDir, Map<String, List<Grammar>> subMapper) throws IOException {
    JarFile jarFile = new JarFile(path);
    Enumeration<JarEntry> dd = jarFile.entries();
    while (dd.hasMoreElements()) {
        JarEntry entry = dd.nextElement();
        if(!entry.isDirectory() && entry.getName().startsWith(templateDir)) {
            log.debug("遍历mapper文件:{}", entry.getName());
            try(InputStream fileIS = jarFile.getInputStream(entry);
                BufferedReader brFile = new BufferedReader(new InputStreamReader(fileIS))) {
                readSqlFile(subMapper, path, entry.getName().substring(templateDir.length()+1), brFile);
            }
        }
    }
  }

  private static void scanDirInResource(String templateDir, Map<String, List<Grammar>> subMapper) throws IOException {
    try (InputStream pathIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(templateDir);
         BufferedReader br = new BufferedReader(new InputStreamReader(pathIS))) {
        for (String filename : br.lines().collect(Collectors.toList())) {
            if (!filename.endsWith(DEFAULT_FILE_TYPE)) {
                scanDirInResource(templateDir + "/" + filename, subMapper);
                continue;
            }

            try(InputStream fileIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(templateDir + "/" + filename);
                BufferedReader brFile = new BufferedReader(new InputStreamReader(fileIS))) {
                readSqlFile(subMapper, templateDir, filename, brFile);
            }
        }
    }
  }

  private static void readSqlFile(Map<String, List<Grammar>> subMapper, String path, String filename, BufferedReader brFile) throws IOException {
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

                String key = fileName.replace("base/", "").concat(".").concat(name);
                log.debug("SQL template load " + key);

                contextBuilder.deleteCharAt(contextBuilder.length() - 1);

                if (subBegin) {
                    subMapper.put(key, SqlTemplate.generateGrammars(contextBuilder.toString()));
                } else {
                    mapper.put(key, SqlTemplate.generateGrammars(contextBuilder.toString()));
                }

                name = "";
                contextBuilder = null;
                subBegin = false;
            }

        }
    }

    private static List<Grammar> generateGrammars(String sql) {
        // 基础转化
        List<BaseKeyValue> baseKeys = Convertor.convertBaseKey(sql);

        // 语法适配
        return Convertor.convertGrammar(baseKeys);
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


