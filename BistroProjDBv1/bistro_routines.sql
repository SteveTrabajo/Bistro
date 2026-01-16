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
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-16 19:00:19
