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
) ENGINE=InnoDB AUTO_INCREMENT=410049 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (410000,'0508707870','user0@example.com','GUEST'),(410001,'0553094235','user1@example.com','GUEST'),(410002,'0542322047','user2@example.com','GUEST'),(410003,'0555918715','user3@example.com','MEMBER'),(410004,'0554226067','user4@example.com','GUEST'),(410005,'0582166941','user5@example.com','MEMBER'),(410006,'0504905582','user6@example.com','GUEST'),(410007,'0507377459','user7@example.com','GUEST'),(410008,'0538606962','user8@example.com','MEMBER'),(410009,'0536960453','user9@example.com','MEMBER'),(410010,'0553871230','user10@example.com','GUEST'),(410011,'0555107245','user11@example.com','MEMBER'),(410012,'0584684531','user12@example.com','MANAGER'),(410013,'0586440561','user13@example.com','GUEST'),(410014,'0504842788','user14@example.com','MEMBER'),(410015,'0532110460','user15@example.com','GUEST'),(410016,'0526279418','user16@example.com','GUEST'),(410017,'0529375710','user17@example.com','GUEST'),(410018,'0548698256','user18@example.com','GUEST'),(410019,'0525443951','user19@example.com','GUEST'),(410020,'0525137722','user20@example.com','MEMBER'),(410021,'0588187926','user21@example.com','GUEST'),(410022,'0557700828','user22@example.com','GUEST'),(410023,'0534679591','user23@example.com','GUEST'),(410024,'0529548432','user24@example.com','GUEST'),(410025,'0542525206','user25@example.com','GUEST'),(410026,'0502839607','user26@example.com','MEMBER'),(410027,'0552065818','user27@example.com','GUEST'),(410028,'0547402509','user28@example.com','GUEST'),(410029,'0558852574','user29@example.com','EMPLOYEE'),(410030,'0555218028','user30@example.com','GUEST'),(410031,'0551192619','user31@example.com','GUEST'),(410032,'0582921859','user32@example.com','GUEST'),(410033,'0585476583','user33@example.com','MEMBER'),(410034,'0538294150','user34@example.com','GUEST'),(410035,'0528612220','user35@example.com','MEMBER'),(410036,'0553997281','user36@example.com','GUEST'),(410037,'0552785277','user37@example.com','MEMBER'),(410038,'0527273233','user38@example.com','MEMBER'),(410039,'0556438436','user39@example.com','MEMBER'),(410040,'0535017343','user40@example.com','GUEST'),(410041,'0505041154','user41@example.com','GUEST'),(410042,'0552321324','user42@example.com','MEMBER'),(410043,'0553109911','user43@example.com','MEMBER'),(410044,'0539852897','user44@example.com','GUEST'),(410045,'0505850700','benayaleib@gmail.com','MANAGER'),(410047,'0505850701',NULL,'GUEST'),(410048,'0505950700',NULL,'GUEST');
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

-- Dump completed on 2026-01-16 19:00:18
