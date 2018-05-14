import com.tollge.sql.SqlTemplate;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author toyer
 * @created 2018-03-14
 */
public class Tester {

    @Test
    public void test() {
        Map<String, Object> params = new HashMap<>();
        params.put("a", "a");
        params.put("b", "bbb");
        params.put("c", "c");
        params.put("list", Arrays.asList("1","1a","1b"));
        System.out.println(SqlTemplate.generateSQL("test.testIf", params));
        System.out.println(SqlTemplate.generateSQL("test.testFor", params));
        System.out.println(SqlTemplate.generateSQL("test.testSubMain", params));
        System.out.println(SqlTemplate.generateSQL("test.testNesting", params));

        Date begin = new Date();
        // 执行性能测试时, 请关闭debug log
        for (int i = 0; i < 100000; i++) {
            SqlTemplate.generateSQL("test.testSubMain", params);
        }
        System.out.println((new Date()).getTime() - begin.getTime());
    }

    @Test
    public void testSome() {
        Map<String, Object> params = new HashMap<>();
        params.put("a", "a");
        params.put("b", "bbb");
        params.put("c", "c");
        params.put("list", Arrays.asList("1","1a","1b"));
        System.out.println(SqlTemplate.generateSQL("test.testSome", params));
    }
}
