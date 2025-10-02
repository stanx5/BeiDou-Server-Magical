package org.gms.client;

import org.gms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * MAC过滤规则操作工具类
 * 提供对macfilters表的增删改查操作
 * 自动管理数据库连接
 */
public class MacFilterHelper {

    /**
     * 获取数据库连接
     * @return 数据库连接
     * @throws SQLException 数据库异常
     */
    private static Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    /**
     * 查询所有MAC过滤规则
     * @return 过滤规则列表
     */
    public static List<String> getAllFilters() {
        List<String> filters = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filters.add(rs.getString("filter"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return filters;
    }

    /**
     * 添加MAC过滤规则
     * @param filter 过滤规则
     * @return 是否添加成功
     */
    public static boolean addFilter(String filter) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO macfilters (filter) VALUES (?)")) {
            ps.setString(1, filter);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查MAC地址列表，返回未匹配过滤规则的MAC地址
     * @param macs 待检查的MAC地址列表
     * @return 未匹配过滤规则的MAC地址列表
     */
    public static List<String> checkMacs(List<String> macs) {
        List<String> unmatchedMacs = new ArrayList<>();
        List<String> filters = getAllFilters();

        for (String mac : macs) {
            boolean matched = false;
            for (String filter : filters) {
                if (mac.matches(filter)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                unmatchedMacs.add(mac);
            }
        }

        return unmatchedMacs;
    }

    /**
     * 删除MAC过滤规则
     * @param filter 过滤规则
     * @return 是否删除成功
     */
    public static boolean deleteFilter(String filter) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM macfilters WHERE filter = ?")) {
            ps.setString(1, filter);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查MAC地址是否匹配任何过滤规则
     * @param mac MAC地址
     * @return 是否匹配
     */
    public static boolean isMacFiltered(String mac) {
        List<String> filters = getAllFilters();
        for (String filter : filters) {
            if (mac.matches(filter)) {
                return true;
            }
        }
        return false;
    }
}
