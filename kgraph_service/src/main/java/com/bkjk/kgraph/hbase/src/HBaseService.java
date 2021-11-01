package com.bkjk.kgraph.hbase.src;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Delete;


public class HBaseService {
    private Logger logger = LoggerFactory.getLogger(HBaseService.class);
    /**
     * 管理员可以做表以及数据的增删改查功能
     */
    private Admin admin = null;
    private Connection connection = null;

    static private String graphNamespace= "kgraph";

    public HBaseService(Configuration conf) {
        try {
            connection = ConnectionFactory.createConnection(conf);
            admin = connection.getAdmin();

            NamespaceDescriptor nsd = NamespaceDescriptor.create(graphNamespace).build();

            admin.createNamespace(nsd);

        } catch (IOException e) {
            logger.error("Fail to get hbase connection!");
        }
    }
    /**
     * 创建表 create <table>, {NAME => <column family>, VERSIONS => <VERSIONS>}
     */
    public boolean createTable(String tableName, List<String> columnFamily) {
        try {
            List<ColumnFamilyDescriptor> cfDesc = new ArrayList<>(columnFamily.size());
            columnFamily.forEach(cf -> {
                cfDesc.add(ColumnFamilyDescriptorBuilder.newBuilder(
                        Bytes.toBytes(cf)).build());
            });
            TableDescriptor tableDesc = TableDescriptorBuilder
                    .newBuilder(TableName.valueOf(tableName))
                    .setColumnFamilies(cfDesc).build();

            if (admin.tableExists(TableName.valueOf(tableName))) {
                logger.debug("table Exists!");
            } else {
                admin.createTable(tableDesc);
                logger.debug("create table Success!");
            }

            return true;
        } catch (IOException e) {
            logger.error(MessageFormat.format("Fail to create table {0}", tableName), e);
            return false;
        } finally {
            close(admin, null, null);
        }
    }
    /**
     * 查询所有表的表名
     */
    public List<String> getAllTableNames() {
        List<String> result = new ArrayList<>();
        try {
            TableName[] tableNames = admin.listTableNames();
            for (TableName tableName : tableNames) {
                result.add(tableName.getNameAsString());
            }
        } catch (IOException e) {
            logger.error("Fail to get hbase tables: ", e);
        } finally {
            close(admin, null, null);
        }
        return result;
    }
    /**
     * 遍历查询指定表中的所有数据
     */
    public Map<String, Map<String, String>> getAllRecords(String tableName) {
        Scan scan = new Scan();
        return this.queryData(tableName, scan);
    }

    public Map<String, Map<String, String>> getRecords(String tableName, String rowKey) {

        Scan scan = new Scan(new Get(rowKey.getBytes()));
        return queryData(tableName, scan);
    }

    public Map<String, Map<String, String>> getRegexRecords(String tableName, String rowKey) {

        Filter filter = new RowFilter(CompareOperator.EQUAL,
                new RegexStringComparator("^" + rowKey + "_"));

        Scan scan = new Scan();
        scan.setFilter(filter);
        return queryData(tableName, scan);
    }

    /**
     * 通过表名及过滤条件查询数据
     */
    public Map<String, Map<String, String>> queryData(String tableName, Scan scan) {
        Map<String, Map<String, String>> result = new HashMap<>();
        ResultScanner rs = null;
        Table table = null;

        try {
            table = getTable(tableName);
            rs = table.getScanner(scan);
            for (Result r : rs) {
                Map<String, String> columnMap = new HashMap<>();
                String rowKey = null;
                for (Cell cell : r.listCells()) {
                    if (rowKey == null) {
                        rowKey = Bytes.toString(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength());
                    }
                    columnMap.put(
                            Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength()),
                            Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength())
                    );
                }
                if (rowKey != null) {
                    result.put(rowKey, columnMap);
                }
            }
        } catch (IOException e) {
            logger.error(MessageFormat.format("Fail to traverse for querying data,tableName:{0}", tableName), e);
        } finally {
            close(null, rs, table);
        }
        return result;
    }

    public void addBatchRecords(String tableName, Map<String, Map<String, Object>> items, Map<String, String> columnFamilyMap) {
        if (items == null || items.isEmpty()) {
            logger.error("[HBase] Adding null/empty item map!");
            return;
        }
        int maxSize = 10000;
        Table table = null;

        try {
            table = getTable(tableName);
            int batchSize = Math.min(maxSize, items.size());
            List<Put> puts = new ArrayList<>(batchSize);
            int handled = 0;

            for (Map.Entry<String, Map<String, Object>> entry : items.entrySet()) {
                String rowKey = entry.getKey();
                Map<String, Object> cells = entry.getValue();

                if (rowKey == null || rowKey.isEmpty()) {
                    logger.error("[HBase] Adding null/empty hashed key! Original key is " + entry.getKey());
                    handled++;
                    continue;
                }

                Put put = new Put(Bytes.toBytes(rowKey));

                cells.forEach((key, value) -> put.addColumn(Bytes.toBytes(columnFamilyMap.get(key)), Bytes.toBytes(key),
                        Bytes.toBytes(value.toString())));

                puts.add(put);
                handled++;

                // 每隔10000,写一次
                if (handled == batchSize) {
                    logger.info("[HBase] Adding " + batchSize + "rows!");
                    table.put(puts);
                    puts = new ArrayList<>(batchSize);
                }
            }
            if (puts.size() > 0) {
                table.put(puts);
            }
        } catch (IOException e) {
            logger.error("[HBase] Error while putting data " + e.getMessage());
        } finally {
            close(null, null, table);
        }
    }


    public void addRecord(String tableName, String rowKey, String familyName, String[] columns, String[] values) {
        Table table = null;
        try {
            table = getTable(tableName);
            addRecord(table, rowKey, tableName, familyName, columns, values);
        } catch (Exception e) {
            logger.error(MessageFormat.format("Fail to insert or update data,tableName:{0},rowKey:{1},familyName:{2}",
                    tableName, rowKey, familyName), e);
        } finally {
            close(null, null, table);
        }
    }

    private void addRecord(Table table, String rowKey, String tableName, String familyName, String[] columns, String[] values) {
        try {
            Put put = new Put(Bytes.toBytes(rowKey));
            if (columns != null && values != null && columns.length == values.length) {
                for (int i = 0; i < columns.length; i++) {
                    if (columns[i] != null && values[i] != null) {
                        put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(columns[i]), Bytes.toBytes(values[i]));
                    } else {
                        throw new NullPointerException(MessageFormat.format(
                                "Column name and data cannot be empty ,column:{0},value:{1}", columns[i], values[i]));
                    }
                }
            }
            table.put(put);
            logger.debug("putData add or update data Success,rowKey:" + rowKey);
            table.close();
        } catch (Exception e) {
            logger.error(MessageFormat.format(
                    "Fail to insert or update data, tableName:{0},rowKey:{1},familyName:{2}",
                    tableName, rowKey, familyName), e);
        }
    }

    public void delRecord(String tableName, String rowKey) {
        Table table = null;

        try {
            table = getTable(tableName);

            List<Delete> delRecords = new ArrayList<>();

            Delete del = new Delete(rowKey.getBytes());

            delRecords.add(del);
            table.delete(delRecords);
        } catch (IOException e) {
            logger.error(MessageFormat.format("Fail to delete data, tableName:{0},rowKey:{1}", tableName, rowKey), e);
        } finally {
            close(null, null, table);
        }
    }

    private Table getTable(String tableName) throws IOException {
        return connection.getTable(TableName.valueOf(tableName));
    }

    private void close(Admin admin, ResultScanner rs, Table table) {
        if (admin == null) {
            return;
        }

        try {
            admin.close();

            if (rs != null) {
                rs.close();
            }

            if (table != null) {
                table.close();
            }
        } catch (IOException e) {
            logger.error("Fail to close: ", e);
        }
    }
}

