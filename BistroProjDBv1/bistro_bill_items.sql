-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: bistro
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bill_items`
--

DROP TABLE IF EXISTS `bill_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_items` (
  `bill_item_id` int NOT NULL AUTO_INCREMENT,
  `billID` int NOT NULL,
  `item_name` varchar(100) NOT NULL,
  `qty` int NOT NULL DEFAULT '1',
  `unit_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`bill_item_id`),
  KEY `idx_bill_items_bill` (`billID`),
  CONSTRAINT `fk_bill_items_bill` FOREIGN KEY (`billID`) REFERENCES `bills` (`billID`) ON DELETE CASCADE,
  CONSTRAINT `chk_bill_price` CHECK ((`unit_price` >= 0)),
  CONSTRAINT `chk_bill_qty` CHECK ((`qty` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=117 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bill_items`
--

LOCK TABLES `bill_items` WRITE;
/*!40000 ALTER TABLE `bill_items` DISABLE KEYS */;
INSERT INTO `bill_items` VALUES (1,840000,'Steak',2,14.69),(2,840000,'Wine',1,87.32),(3,840000,'Pasta',1,102.60),(4,840001,'Pasta',2,105.21),(5,840001,'Steak',1,36.52),(6,840002,'Steak',1,33.29),(7,840002,'Dessert',2,39.36),(8,840003,'Salad',1,88.16),(9,840003,'Soft Drink',1,63.68),(10,840004,'Soft Drink',3,50.40),(11,840004,'Soup',1,13.19),(12,840004,'Beer',3,139.50),(13,840005,'Soft Drink',3,50.32),(14,840005,'Soft Drink',1,132.18),(15,840006,'Coffee',1,81.84),(16,840006,'Soup',3,82.72),(17,840007,'Soup',3,125.17),(18,840007,'Fish',3,107.93),(19,840008,'Pasta',1,88.36),(20,840008,'Soup',3,112.66),(21,840008,'Fish',3,137.98),(22,840008,'Steak',2,127.14),(23,840009,'Wine',1,36.56),(24,840009,'Soft Drink',2,98.70),(25,840009,'Salad',3,84.85),(26,840009,'Fish',3,118.72),(27,840010,'Steak',1,98.12),(28,840010,'Pizza',1,30.86),(29,840010,'Pasta',1,137.94),(30,840010,'Beer',1,70.29),(31,840011,'Pizza',1,114.93),(32,840011,'Coffee',3,71.46),(33,840011,'Wine',1,129.44),(34,840011,'Steak',2,29.07),(35,840011,'Dessert',3,79.98),(36,840012,'Pizza',3,47.50),(37,840012,'Wine',2,92.22),(38,840012,'Burger',2,21.92),(39,840012,'Burger',1,46.78),(40,840013,'Beer',2,73.05),(41,840013,'Soup',2,108.44),(42,840014,'Beer',3,15.45),(43,840014,'Fish',3,84.24),(44,840014,'Pasta',1,94.27),(45,840015,'Salad',2,25.84),(46,840015,'Pasta',2,85.70),(47,840016,'Coffee',1,67.74),(48,840016,'Coffee',3,135.58),(49,840016,'Burger',3,115.45),(50,840016,'Wine',2,71.50),(51,840016,'Coffee',3,46.35),(52,840017,'Burger',2,47.80),(53,840017,'Beer',3,124.65),(54,840017,'Wine',1,78.13),(55,840017,'Pizza',1,42.89),(56,840018,'Dessert',3,79.96),(57,840018,'Fish',3,82.45),(58,840018,'Soup',3,70.08),(59,840018,'Burger',2,41.51),(60,840019,'Steak',2,74.94),(61,840019,'Beer',3,99.87),(62,840020,'Burger',3,91.49),(63,840020,'Beer',3,24.70),(64,840020,'Pizza',3,39.01),(65,840020,'Soft Drink',1,32.10),(66,840020,'Salad',3,21.61),(67,840021,'Steak',3,41.88),(68,840021,'Burger',3,87.49),(69,840021,'Pizza',2,26.69),(70,840021,'Pizza',3,31.07),(71,840021,'Burger',1,21.14),(72,840022,'Pasta',1,41.30),(73,840022,'Soft Drink',3,87.15),(74,840022,'Pasta',3,117.14),(75,840022,'Beer',1,109.68),(76,840023,'Dessert',2,64.27),(77,840023,'Soup',3,25.39),(78,840024,'Steak',1,136.56),(79,840024,'Beer',1,102.85),(80,840024,'Burger',3,13.75),(81,840024,'Fish',1,40.76),(82,840024,'Steak',2,133.22),(83,840025,'Beer',3,107.53),(84,840025,'Salad',1,47.36),(85,840025,'Fish',2,131.58),(86,840026,'Dessert',2,123.31),(87,840026,'Coffee',1,85.19),(88,840026,'Pizza',2,17.59),(89,840026,'Coffee',2,102.27),(90,840026,'Fish',2,138.52),(91,840027,'Beer',3,62.88),(92,840027,'Steak',2,121.10),(93,840027,'Wine',2,134.13),(94,840027,'Soft Drink',3,125.59),(95,840028,'Pasta',2,113.91),(96,840028,'Burger',2,59.98),(97,840029,'Pizza',2,93.65),(98,840029,'Dessert',2,97.36),(99,840029,'Dessert',2,98.73),(100,840030,'Fish',3,57.95),(101,840030,'Burger',3,49.79),(102,840030,'Soft Drink',2,47.13),(103,840031,'Soft Drink',3,54.89),(104,840031,'Burger',1,85.93),(105,840032,'Steak',2,59.31),(106,840032,'Soup',3,75.30),(107,840033,'Soft Drink',2,40.05),(108,840033,'Coffee',1,47.97),(109,840033,'Steak',2,84.60),(110,840033,'Soft Drink',3,83.38),(111,840033,'Pasta',1,53.48),(112,840034,'Pasta',2,111.33),(113,840034,'Soup',3,57.22),(114,840034,'Dessert',3,80.42),(115,840034,'Burger',1,54.55),(116,840034,'Soup',1,98.15);
/*!40000 ALTER TABLE `bill_items` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-16 19:00:18
