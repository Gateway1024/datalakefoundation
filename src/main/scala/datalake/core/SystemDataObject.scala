package datalake.core

import datalake.metadata._
import org.apache.spark.sql.{ DataFrame, SparkSession, Row }
import io.delta.tables._
import org.apache.spark.sql.types.{ StructType, DataType, StructField }
import java.sql.Timestamp
import org.apache.spark.sql.Dataset

case class SystemDataColumn (
    name: String,
    data_type: DataType,
    nullable: Boolean,
    part_of_partition: Boolean
)

class SystemDataTable_Definition(name: String, schema: List[SystemDataColumn]) extends Serializable {

  final def Name: String =
    this.name

  final def Columns: List[SystemDataColumn] =
    this.schema

  final def Schema: StructType =
    StructType(
      schema.map(f => StructField(f.name, f.data_type, f.nullable))
    )
}

class SystemDataObject(table_definition: SystemDataTable_Definition)(implicit environment: Environment) extends Serializable {
  private val spark: SparkSession = SparkSession.builder.enableHiveSupport().getOrCreate()
  import spark.implicits._

  val deltaTablePath = s"${environment.RootFolder}/system/${table_definition.Name}"
  val partition = table_definition.Columns.filter(c => c.part_of_partition == true).map(c => c.name)

  final def Append(rows: Seq[Row]): Unit = {
    var data = spark.sparkContext.parallelize(rows)
    var append_df = spark.createDataFrame(data, table_definition.Schema)

    append_df.write.format("delta").partitionBy(partition:_*).mode("append").save(deltaTablePath)
  }

  final def Append(row: Row): Unit = {
    var rows = Seq(row)
    this.Append(rows)
  }

  final def getDataFrame: DataFrame =
    spark.read.format("delta").load(deltaTablePath)

}
