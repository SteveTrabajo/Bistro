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
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'0500000001','user1@mail.com','GUEST'),(2,'0500000002','user2@mail.com','GUEST'),(3,'0500000003','user3@mail.com','GUEST'),(4,'0500000004','user4@mail.com','GUEST'),(5,'0500000005','user5@mail.com','GUEST'),(6,'0500000006','user6@mail.com','GUEST'),(7,'0500000007','user7@mail.com','GUEST'),(8,'0500000008','user8@mail.com','GUEST'),(9,'0500000009','user9@mail.com','GUEST'),(10,'0500000010','user10@mail.com','GUEST'),(11,'0500000011','user11@mail.com','GUEST'),(12,'0500000012','user12@mail.com','GUEST'),(13,'0500000013','user13@mail.com','GUEST'),(14,'0500000014','user14@mail.com','GUEST'),(15,'0500000015','user15@mail.com','GUEST'),(16,'0500000016','user16@mail.com','GUEST'),(17,'0500000017','user17@mail.com','GUEST'),(18,'0500000018','user18@mail.com','GUEST'),(19,'0500000019','user19@mail.com','GUEST'),(20,'0500000020','user20@mail.com','GUEST'),(21,'0500000021','user21@mail.com','GUEST'),(22,'0500000022','user22@mail.com','GUEST'),(23,'0500000023','user23@mail.com','GUEST'),(24,'0500000024','user24@mail.com','GUEST'),(25,'0500000025','user25@mail.com','GUEST'),(26,'0500000026','user26@mail.com','GUEST'),(27,'0500000027','user27@mail.com','GUEST'),(28,'0500000028','user28@mail.com','GUEST'),(29,'0500000029','user29@mail.com','GUEST'),(30,'0500000030','user30@mail.com','GUEST'),(31,NULL,'member31@mail.com','MEMBER'),(32,NULL,'member32@mail.com','MEMBER'),(33,NULL,'member33@mail.com','MEMBER'),(34,NULL,'member34@mail.com','MEMBER'),(35,NULL,'member35@mail.com','MEMBER'),(36,NULL,'member36@mail.com','MEMBER'),(37,NULL,'member37@mail.com','MEMBER'),(38,NULL,'member38@mail.com','MEMBER'),(39,NULL,'member39@mail.com','MEMBER'),(40,NULL,'member40@mail.com','MEMBER'),(41,NULL,'member41@mail.com','MEMBER'),(42,NULL,'member42@mail.com','MEMBER'),(43,NULL,'member43@mail.com','MEMBER'),(44,NULL,'member44@mail.com','MEMBER'),(45,NULL,'member45@mail.com','MEMBER'),(46,NULL,'member46@mail.com','MEMBER'),(47,NULL,'member47@mail.com','MEMBER'),(48,NULL,'member48@mail.com','MEMBER'),(49,NULL,'member49@mail.com','MEMBER'),(50,NULL,'member50@mail.com','MEMBER'),(51,'0508562703','steve@admin.com','MANAGER');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-17 12:49:36
