package quartzplus.core.quartz;

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static org.quartz.TriggerKey.triggerKey;

public class GroupBalanceTriggerDelegate extends StdJDBCDelegate {

    private static final String additionalQuery = "AND NOT EXISTS(SELECT 1 FROM {0}FIRED_TRIGGERS qft WHERE qft.JOB_GROUP = qt.JOB_GROUP AND qft.SCHED_NAME = qt.SCHED_NAME)";

    private String buildSql() {
        return "SELECT TRIGGER_NAME, TRIGGER_GROUP, NEXT_FIRE_TIME, PRIORITY FROM {0}TRIGGERS qt " +
            "WHERE SCHED_NAME = {1} AND TRIGGER_STATE = ? AND NEXT_FIRE_TIME <= ? AND (MISFIRE_INSTR = -1 OR (MISFIRE_INSTR != -1 AND NEXT_FIRE_TIME >= ?)) " +
            additionalQuery + " ORDER BY NEXT_FIRE_TIME ASC, PRIORITY DESC";
    }

    @Override
    public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan, long noEarlierThan, int maxCount)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<TriggerKey> nextTriggers = new LinkedList<TriggerKey>();
        try {
            ps = conn.prepareStatement(rtp(buildSql()));

            // Set max rows to retrieve
            if (maxCount < 1)
                maxCount = 1; // we want at least one trigger back.
            ps.setMaxRows(maxCount);

            // Try to give jdbc driver a hint to hopefully not pull over more than the few rows we actually need.
            // Note: in some jdbc drivers, such as MySQL, you must set maxRows before fetchSize, or you get exception!
            ps.setFetchSize(maxCount);

            ps.setString(1, STATE_WAITING);
            ps.setBigDecimal(2, new BigDecimal(String.valueOf(noLaterThan)));
            ps.setBigDecimal(3, new BigDecimal(String.valueOf(noEarlierThan)));
            rs = ps.executeQuery();

            while (rs.next() && nextTriggers.size() < maxCount) {
                nextTriggers.add(triggerKey(
                    rs.getString(COL_TRIGGER_NAME),
                    rs.getString(COL_TRIGGER_GROUP)));
            }

            if (CollectionUtils.isNotEmpty(nextTriggers)) {
                return nextTriggers;
            }

            return super.selectTriggerToAcquire(conn, noLaterThan, noEarlierThan, maxCount);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }
}
