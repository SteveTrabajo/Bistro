-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: bistro
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bill_items`
--

LOCK TABLES `bill_items` WRITE;
/*!40000 ALTER TABLE `bill_items` DISABLE KEYS */;
INSERT INTO `bill_items` VALUES (1,1,'Bistro Burger',2,68.00),(2,1,'Greek Salad',1,44.00),(3,1,'Soft Drink',2,20.00);
/*!40000 ALTER TABLE `bill_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bills`
--

DROP TABLE IF EXISTS `bills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bills` (
  `billID` int NOT NULL AUTO_INCREMENT,
  `session_id` int NOT NULL,
  `billSum` decimal(10,2) NOT NULL,
  `subtotal_amount` decimal(10,2) NOT NULL DEFAULT '0.00',
  `discount_percent` decimal(5,2) NOT NULL DEFAULT '0.00',
  `billDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `paid_at` datetime DEFAULT NULL,
  `payment_method` enum('CASH','CREDIT') DEFAULT NULL,
  `payment_status` enum('UNPAID','PAID') NOT NULL DEFAULT 'UNPAID',
  `transaction_id` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`billID`),
  UNIQUE KEY `session_id` (`session_id`),
  CONSTRAINT `fk_bills_session` FOREIGN KEY (`session_id`) REFERENCES `table_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bills`
--

LOCK TABLES `bills` WRITE;
/*!40000 ALTER TABLE `bills` DISABLE KEYS */;
INSERT INTO `bills` VALUES (1,1,198.00,220.00,10.00,'2026-01-17 19:55:00','2026-01-17 19:55:10','CREDIT','PAID','TXN-260117-900003'),(2,4,0.00,0.00,0.00,'2026-01-18 01:53:12',NULL,NULL,'UNPAID',NULL),(3,5,0.00,0.00,0.00,'2026-01-18 01:55:03',NULL,NULL,'UNPAID',NULL);
/*!40000 ALTER TABLE `bills` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `user_id` int NOT NULL,
  `member_code` int NOT NULL,
  `f_name` varchar(50) NOT NULL,
  `l_name` varchar(50) NOT NULL,
  `address` varchar(150) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_member_code` (`member_code`),
  CONSTRAINT `fk_member_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
INSERT INTO `members` VALUES (1,300101,'Yael','Levy','Tel Aviv, Dizengoff 120'),(2,300102,'Dan','Cohen','Haifa, Carmel 15'),(3,300103,'Noa','Mizrahi','Rishon LeZion, Rothschild 8'),(7,642354,'admin','admin','admin');
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `opening_hours_special`
--

DROP TABLE IF EXISTS `opening_hours_special`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `opening_hours_special` (
  `special_date` date NOT NULL,
  `holiday_name` varchar(100) NOT NULL,
  `is_closed` tinyint NOT NULL DEFAULT '0',
  `open_time` time DEFAULT NULL,
  `close_time` time DEFAULT NULL,
  PRIMARY KEY (`special_date`),
  CONSTRAINT `chk_spec_hours` CHECK ((((`is_closed` = 1) and (`open_time` is null) and (`close_time` is null)) or ((`is_closed` = 0) and (`open_time` is not null) and (`close_time` is not null) and (`open_time` < `close_time`))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `opening_hours_special`
--

LOCK TABLES `opening_hours_special` WRITE;
/*!40000 ALTER TABLE `opening_hours_special` DISABLE KEYS */;
INSERT INTO `opening_hours_special` VALUES ('2026-02-14','Valentines Day',0,'17:00:00','23:30:00'),('2026-04-24','Independence Day',1,NULL,NULL);
/*!40000 ALTER TABLE `opening_hours_special` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `opening_hours_weekly`
--

DROP TABLE IF EXISTS `opening_hours_weekly`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `opening_hours_weekly` (
  `day_of_week` tinyint NOT NULL,
  `open_time` time NOT NULL,
  `close_time` time NOT NULL,
  PRIMARY KEY (`day_of_week`),
  CONSTRAINT `chk_week_hours` CHECK ((`open_time` < `close_time`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `opening_hours_weekly`
--

LOCK TABLES `opening_hours_weekly` WRITE;
/*!40000 ALTER TABLE `opening_hours_weekly` DISABLE KEYS */;
INSERT INTO `opening_hours_weekly` VALUES (1,'08:00:00','22:00:00'),(2,'08:00:00','22:00:00'),(3,'08:00:00','22:00:00'),(4,'08:00:00','22:00:00'),(5,'10:00:00','23:00:00'),(6,'10:00:00','23:00:00'),(7,'10:00:00','22:00:00');
/*!40000 ALTER TABLE `opening_hours_weekly` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `order_number` int NOT NULL AUTO_INCREMENT,
  `confirmation_code` varchar(50) NOT NULL,
  `user_id` int NOT NULL,
  `number_of_guests` int NOT NULL,
  `order_date` date DEFAULT NULL,
  `order_time` time DEFAULT NULL,
  `date_of_placing_order` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `order_type` enum('RESERVATION','WAITLIST') NOT NULL,
  `status` enum('PENDING','NOTIFIED','SEATED','CANCELLED','NO_SHOW','COMPLETED') NOT NULL DEFAULT 'PENDING',
  `notified_at` datetime DEFAULT NULL,
  `cancelled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`order_number`),
  UNIQUE KEY `uq_orders_confirmation` (`confirmation_code`),
  KEY `idx_orders_user` (`user_id`),
  KEY `idx_orders_slot` (`order_date`,`order_time`),
  KEY `idx_orders_status` (`status`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_guests_amount` CHECK ((`number_of_guests` > 0)),
  CONSTRAINT `chk_order_slot_rules` CHECK ((((`order_type` = _utf8mb4'WAITLIST') and (`order_date` is null) and (`order_time` is null)) or ((`order_type` = _utf8mb4'RESERVATION') and (`order_date` is not null) and (`order_time` is not null) and (second(`order_time`) = 0) and (minute(`order_time`) in (0,30)))))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,'R-900001',1,2,'2026-01-25','18:30:00','2026-01-18 10:00:00','RESERVATION','PENDING',NULL,NULL),(2,'R-900002',4,4,'2026-01-26','19:00:00','2026-01-18 10:05:00','RESERVATION','CANCELLED',NULL,NULL),(3,'R-900003',2,3,'2026-01-17','18:00:00','2026-01-15 12:10:00','RESERVATION','COMPLETED',NULL,NULL),(4,'R-900004',5,2,'2026-01-16','20:00:00','2026-01-14 09:30:00','RESERVATION','NO_SHOW',NULL,NULL),(5,'R-900005',3,2,'2026-01-18','19:30:00','2026-01-18 09:00:00','RESERVATION','SEATED',NULL,NULL),(6,'W-700101',6,3,NULL,NULL,'2026-01-18 01:51:39','WAITLIST','PENDING',NULL,NULL),(7,'W-700102',4,2,NULL,NULL,'2026-01-18 01:51:39','WAITLIST','NOTIFIED',NULL,NULL),(8,'W-700103',5,4,NULL,NULL,'2026-01-18 01:51:39','WAITLIST','CANCELLED',NULL,NULL),(9,'W-792774',1,9,NULL,NULL,'2026-01-18 01:53:12','WAITLIST','SEATED',NULL,NULL),(10,'W-884413',8,12,NULL,NULL,'2026-01-18 01:54:13','WAITLIST','CANCELLED',NULL,'2026-01-18 01:54:50'),(11,'W-949271',8,6,NULL,NULL,'2026-01-18 01:55:03','WAITLIST','SEATED',NULL,NULL);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `report_id` int NOT NULL AUTO_INCREMENT,
  `report_type` varchar(20) NOT NULL,
  `report_year` smallint NOT NULL,
  `report_month` tinyint NOT NULL,
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `payload` longblob NOT NULL,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uq_report_type_year_month` (`report_type`,`report_year`,`report_month`),
  CONSTRAINT `reports_chk_1` CHECK ((`report_month` between 1 and 12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reports`
--

LOCK TABLES `reports` WRITE;
/*!40000 ALTER TABLE `reports` DISABLE KEYS */;
/*!40000 ALTER TABLE `reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `staff_accounts`
--

DROP TABLE IF EXISTS `staff_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_accounts` (
  `user_id` int NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_staff_username` (`username`),
  CONSTRAINT `fk_staff_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff_accounts`
--

LOCK TABLES `staff_accounts` WRITE;
/*!40000 ALTER TABLE `staff_accounts` DISABLE KEYS */;
INSERT INTO `staff_accounts` VALUES (7,'admin','Iw0eHRw=');
/*!40000 ALTER TABLE `staff_accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `table_sessions`
--

DROP TABLE IF EXISTS `table_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_sessions` (
  `session_id` int NOT NULL AUTO_INCREMENT,
  `order_number` int NOT NULL,
  `tableNum` int NOT NULL,
  `seated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expected_end_at` datetime DEFAULT NULL,
  `left_at` datetime DEFAULT NULL,
  `end_reason` enum('PAID','LEFT','NO_SHOW') DEFAULT NULL,
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `order_number` (`order_number`),
  KEY `idx_sessions_table_open` (`tableNum`,`left_at`),
  CONSTRAINT `fk_sessions_order` FOREIGN KEY (`order_number`) REFERENCES `orders` (`order_number`) ON DELETE CASCADE,
  CONSTRAINT `fk_sessions_table` FOREIGN KEY (`tableNum`) REFERENCES `tables` (`tableNum`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `table_sessions`
--

LOCK TABLES `table_sessions` WRITE;
/*!40000 ALTER TABLE `table_sessions` DISABLE KEYS */;
INSERT INTO `table_sessions` VALUES (1,3,3,'2026-01-17 18:05:00','2026-01-17 20:00:00','2026-01-17 19:55:00','PAID'),(2,4,1,'2026-01-16 20:05:00','2026-01-16 21:30:00','2026-01-16 20:20:00','NO_SHOW'),(3,5,2,'2026-01-18 19:35:00','2026-01-18 21:00:00',NULL,NULL),(4,9,8,'2026-01-18 01:53:12','2026-01-18 03:53:12',NULL,NULL),(5,11,5,'2026-01-18 01:55:03','2026-01-18 03:55:03',NULL,NULL);
/*!40000 ALTER TABLE `table_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tables`
--

DROP TABLE IF EXISTS `tables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tables` (
  `tableNum` int NOT NULL,
  `capacity` int NOT NULL,
  PRIMARY KEY (`tableNum`),
  CONSTRAINT `chk_tables_capacity` CHECK ((`capacity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tables`
--

LOCK TABLES `tables` WRITE;
/*!40000 ALTER TABLE `tables` DISABLE KEYS */;
INSERT INTO `tables` VALUES (1,2),(2,2),(3,4),(4,4),(5,6),(6,6),(7,8),(8,10);
/*!40000 ALTER TABLE `tables` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `tables_current_status`
--

DROP TABLE IF EXISTS `tables_current_status`;
/*!50001 DROP VIEW IF EXISTS `tables_current_status`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `tables_current_status` AS SELECT 
 1 AS `tableNum`,
 1 AS `capacity`,
 1 AS `occupied_now`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `phoneNumber` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `type` enum('GUEST','MEMBER','EMPLOYEE','MANAGER') NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_users_phone` (`phoneNumber`),
  UNIQUE KEY `uq_users_email` (`email`),
  CONSTRAINT `chk_user_contact` CHECK (((`phoneNumber` is not null) or (`email` is not null)))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'0509000101','yael.levy@mail.com','MEMBER'),(2,'0509000102','dan.cohen@mail.com','MEMBER'),(3,'0509000103','noa.mizrahi@mail.com','MEMBER'),(4,'0509000201',NULL,'GUEST'),(5,NULL,'guest.alex@mail.com','GUEST'),(6,'0509000203','guest.ron@mail.com','GUEST'),(7,'0501234567','admin@admin.com','MANAGER'),(8,'0508562703',NULL,'GUEST');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `view_waitlist_queue`
--

DROP TABLE IF EXISTS `view_waitlist_queue`;
/*!50001 DROP VIEW IF EXISTS `view_waitlist_queue`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `view_waitlist_queue` AS SELECT 
 1 AS `queue_position`,
 1 AS `confirmation_code`,
 1 AS `user_id`,
 1 AS `number_of_guests`,
 1 AS `date_of_placing_order`,
 1 AS `quoted_wait_time`,
 1 AS `minutes_waiting_so_far`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `waiting_list`
--

DROP TABLE IF EXISTS `waiting_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `waiting_list` (
  `confirmation_code` varchar(50) NOT NULL,
  `quoted_wait_time` int DEFAULT NULL,
  `priority` int NOT NULL DEFAULT '2',
  `requested_time` datetime DEFAULT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `wl_status` enum('WAITING','NOTIFIED','SEATED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'WAITING',
  PRIMARY KEY (`confirmation_code`),
  KEY `idx_waiting_queue` (`wl_status`,`priority`,`requested_time`,`joined_at`),
  CONSTRAINT `fk_waitlist_orders_code` FOREIGN KEY (`confirmation_code`) REFERENCES `orders` (`confirmation_code`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `waiting_list`
--

LOCK TABLES `waiting_list` WRITE;
/*!40000 ALTER TABLE `waiting_list` DISABLE KEYS */;
INSERT INTO `waiting_list` VALUES ('W-700101',35,2,NULL,'2026-01-18 01:51:39','WAITING'),('W-700102',20,1,NULL,'2026-01-18 01:51:39','NOTIFIED'),('W-700103',45,3,NULL,'2026-01-18 01:51:39','CANCELLED'),('W-884413',60,2,'2026-01-18 01:54:13','2026-01-18 01:54:12','CANCELLED');
/*!40000 ALTER TABLE `waiting_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `tables_current_status`
--

/*!50001 DROP VIEW IF EXISTS `tables_current_status`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `tables_current_status` AS select `t`.`tableNum` AS `tableNum`,`t`.`capacity` AS `capacity`,exists(select 1 from `table_sessions` `s` where ((`s`.`tableNum` = `t`.`tableNum`) and (`s`.`left_at` is null))) AS `occupied_now` from `tables` `t` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_waitlist_queue`
--

/*!50001 DROP VIEW IF EXISTS `view_waitlist_queue`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `view_waitlist_queue` AS select row_number() OVER (ORDER BY `o`.`date_of_placing_order` )  AS `queue_position`,`wl`.`confirmation_code` AS `confirmation_code`,`o`.`user_id` AS `user_id`,`o`.`number_of_guests` AS `number_of_guests`,`o`.`date_of_placing_order` AS `date_of_placing_order`,`wl`.`quoted_wait_time` AS `quoted_wait_time`,timestampdiff(MINUTE,`o`.`date_of_placing_order`,now()) AS `minutes_waiting_so_far` from (`waiting_list` `wl` join `orders` `o` on((`wl`.`confirmation_code` = `o`.`confirmation_code`))) where (`o`.`status` = 'PENDING') order by `o`.`date_of_placing_order` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-18  2:02:16
