SET @stock_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'products'
      AND COLUMN_NAME = 'stock'
);

SET @drop_stock_sql = IF(
    @stock_column_exists > 0,
    'ALTER TABLE products DROP COLUMN stock',
    'SELECT 1'
);

PREPARE stmt FROM @drop_stock_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
