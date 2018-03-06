package bletsos.panos

import com.databricks.spark.sql.perf.tpcds.TPCDSTables
import com.typesafe.config.{Config, ConfigFactory}

import org.apache.spark.sql.SparkSession

object App {
  def main(args: Array[String]): Unit = {
    val conf: Config = ConfigFactory.load

    val spark: SparkSession = SparkSession
      .builder()
      .master(conf.getString("spark.master"))
      .appName("generate-tpcds-data")
      .getOrCreate()

    // root directory of location to create data in.
    val rootDir = conf.getString("rootDir")
    // name of database to create.
    val databaseName = "tpcdsParquet"
    // scaleFactor defines the size of the dataset to generate (in GB).
    val scaleFactor = conf.getString("scaleFactor")
    // valid spark format like parquet "parquet".
    val format = conf.getString("format")
    // Run:
    val tables = new TPCDSTables(spark.sqlContext,
      dsdgenDir = conf.getString("dsdgenDir"), // location of dsdgen
      scaleFactor = scaleFactor,
      useDoubleForDecimal = false, // true to replace DecimalType with DoubleType
      useStringForDate = false) // true to replace DateType with StringType

    tables.genData(
      location = rootDir,
      format = format,
      overwrite = true, // overwrite the data that is already there
      partitionTables = true, // create the partitioned fact tables
      clusterByPartitionColumns = true, // shuffle to get partitions coalesced into single files.
      filterOutNullPartitionValues = false, // true to filter out the partition with NULL key value
      tableFilter = "", // "" means generate all tables
      numPartitions = 100) // how many dsdgen partitions to run - number of input tasks.

    tables.createExternalTables(
      rootDir, "parquet", databaseName, overwrite = true, discoverPartitions = true)
  }
}