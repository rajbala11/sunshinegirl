from pyspark.sql.functions import col,lit,array,struct, explode
#from pyspark.sql.types import Struct
data = [
    (0, 95, 100, 105),
    (1, 70, None, 80)
]
columns = ["product_id", "store1", "store2", "store3"]
df = spark.createDataFrame(data, columns)
df1 = df.withColumn(
    "store_price",
    array(struct(lit("store1").alias("store"), col("store1").alias("price")),
    struct(lit("store2").alias("store"), col("store2").alias("price")),
    struct(lit("store3").alias("store"), col("store3").alias("price"))
    )).withColumn("store_price", explode(col("store_price")))
    
df2 = df1.select("product_id", "store_price.store", "store_price.price").filter(col("price").isNotNull())
display(df2)
#df2 = df1.select("product_id", "store_price")
