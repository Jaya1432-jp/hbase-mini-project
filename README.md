  
# HBase–Hadoop & HBase–Hive Integration Mini Project

A hands-on project demonstrating two core integrations in the Hadoop ecosystem:

1. **HBase–Hadoop**: bulk-loading data from HDFS into HBase using a custom MapReduce job.
2. **HBase–Hive**: querying and writing HBase data through HiveQL using an external table, with zero data copying.

## Stack

- Hadoop 3.3.6
- HBase 2.6.6
- Hive 3.1.2
- Java 8

## Part 1 — CSV → HDFS → MapReduce → HBase

A sample `employees.csv` (5 rows: emp_id, name, dept, salary) is uploaded to HDFS, then loaded into an HBase table via a MapReduce job.

**Files:**
- `CsvToHBaseMapper.java` — reads CSV lines from HDFS (`TextInputFormat`) and emits `Put` operations.
- `CsvToHBaseDriver.java` — configures the job (`TableOutputFormat`, map-only, targets the `employees` table).

**HBase table schema:**
```
create 'employees', 'personal', 'work'
```
- `personal:name`
- `work:dept`
- `work:salary`

**Run it:**
```bash
# Compile
javac -classpath "$(hadoop classpath):$(hbase classpath)" -d build CsvToHBaseMapper.java CsvToHBaseDriver.java

# Package
jar -cvf csvtohbase.jar -C build/ .

# Run
export HADOOP_CLASSPATH=$(hbase classpath):$(find /usr/local/hbase/lib -name "*.jar" | tr '\n' ':')
yarn jar csvtohbase.jar CsvToHBaseDriver /user/hadoop/input

# IMPORTANT: unset before using hbase shell afterward
unset HADOOP_CLASSPATH
```

**Verify:**
```
hbase shell
scan 'employees'
```

## Part 2 — Hive on top of HBase

An external Hive table maps directly onto the `employees` HBase table via `HBaseStorageHandler` — no ETL, no data duplication.

```sql
CREATE EXTERNAL TABLE hive_employees (
  emp_id STRING,
  name STRING,
  dept STRING,
  salary STRING
)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
  "hbase.columns.mapping" = ":key,personal:name,work:dept,work:salary"
)
TBLPROPERTIES ("hbase.table.name" = "employees");
```

Demonstrated capabilities:
- **Read**: `SELECT * FROM hive_employees;`
- **Aggregate**: `GROUP BY` with `COUNT` and `AVG`, runs as a MapReduce job.
- **Write-back**: `INSERT INTO TABLE hive_employees VALUES (...)` writes straight into the underlying HBase table.
- **Join**: joining the HBase-backed table with a native Hive table (`department_budget`) on `dept`.

## What this demonstrates

- HBase's HFiles live on HDFS, so any HDFS-resident data is a MapReduce job away from being ingested into HBase.
- `TableMapReduceUtil` wires up the HBase-specific serialization needed for MapReduce jobs talking to HBase.
- Hive can layer SQL-style analytics directly on top of HBase's low-latency operational store, including two-way reads/writes and joins with regular Hive tables — without copying data between systems.

## Notes / gotchas hit along the way

- `TableOutputFormat`/`TableMapReduceUtil` require `hbase-mapreduce`, `hbase-server`, and other HBase jars on both the **compile** and **runtime** (`HADOOP_CLASSPATH`) classpath — `hbase classpath` alone doesn't always include all of them.
- A mapper reading plain text (`TextInputFormat`) should extend `Mapper`, not `TableMapper` (the latter is for reading *from* an HBase table, not writing to one).
- Leaving a broad `HADOOP_CLASSPATH` set can break `hbase shell` (jline version conflicts) — unset it before launching the shell.
- A half-initialized Derby metastore (`Table/View 'VERSION' does not exist`) is fixed by deleting `metastore_db/` and re-running `schematool -dbType derby -initSchema`.
