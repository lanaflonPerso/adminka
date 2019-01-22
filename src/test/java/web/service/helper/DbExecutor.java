package web.service.helper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DbExecutor {
    public static void execute(DataSource dataSource, String... scripts) throws SQLException {
        Connection db = dataSource.getConnection();
        db.setAutoCommit(false);
        for( String str: scripts) {
            ScriptUtils.executeSqlScript(db, new ClassPathResource(str));
        }
        db.setAutoCommit(true);
        db.close();
    }
}
