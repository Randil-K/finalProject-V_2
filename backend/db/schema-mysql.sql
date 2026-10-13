
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account_documents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content_type` varchar(255) NOT NULL,
  `kind` varchar(20) DEFAULT NULL,
  `original_name` varchar(150) NOT NULL,
  `size_bytes` bigint(20) NOT NULL,
  `stored_name` varchar(255) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs376h693q3xigdi0qbjhwpnby` (`stored_name`),
  KEY `FKe6sj2xoswxmu9s3xq4bcwftu5` (`user_id`),
  CONSTRAINT `FKe6sj2xoswxmu9s3xq4bcwftu5` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alerts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `body` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `critical` tinyint(1) NOT NULL DEFAULT 0,
  `project_id` bigint(20) DEFAULT NULL,
  `radius_km` double DEFAULT NULL,
  `read_flag` bit(1) NOT NULL,
  `reply` varchar(20) DEFAULT NULL,
  `report_id` bigint(20) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK419j42c8tkd9ja320j2ah62eo` (`recipient_id`),
  CONSTRAINT `FK419j42c8tkd9ja320j2ah62eo` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=694 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cleanup_projects` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `completed_at` datetime(6) DEFAULT NULL,
  `completion_percentage` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `debris_removed_kg` double DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `divers_needed` int(11) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `location_name` varchar(255) NOT NULL,
  `longitude` double DEFAULT NULL,
  `progress_open_to_participants` bit(1) NOT NULL DEFAULT b'0',
  `province` varchar(255) DEFAULT NULL,
  `recruitment_radius_km` double DEFAULT NULL,
  `recruitment_widened_at` datetime(6) DEFAULT NULL,
  `reference` varchar(255) NOT NULL,
  `resources_finalized_at` datetime(6) DEFAULT NULL,
  `resources_gathered_at` datetime(6) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `volunteers_needed` int(11) DEFAULT NULL,
  `owner_id` bigint(20) NOT NULL,
  `report_id` bigint(20) DEFAULT NULL,
  `resources_finalized_by_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKeirb9dvc078wsq7xnelh5xgy6` (`reference`),
  KEY `FKa3jvmn2jo0dfc69fdd3kubvra` (`owner_id`),
  KEY `FKked56xoc81ct4pcxhb332p43r` (`report_id`),
  KEY `FKcigg8frt8ed1uv52mqg0f45em` (`resources_finalized_by_id`),
  CONSTRAINT `FKa3jvmn2jo0dfc69fdd3kubvra` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKcigg8frt8ed1uv52mqg0f45em` FOREIGN KEY (`resources_finalized_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKked56xoc81ct4pcxhb332p43r` FOREIGN KEY (`report_id`) REFERENCES `pollution_reports` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=390 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `comment_reactions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `type` varchar(20) NOT NULL,
  `comment_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjeq4skdrf5vch9lr8bwpupubj` (`comment_id`,`user_id`),
  KEY `FK2t2mv78fm49m4lni9gih7kkaa` (`user_id`),
  CONSTRAINT `FK2t2mv78fm49m4lni9gih7kkaa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKbf6904sdbxretk0gv6p954lnx` FOREIGN KEY (`comment_id`) REFERENCES `report_comments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `diver_preferred_regions` (
  `diver_profile_id` bigint(20) NOT NULL,
  `region` varchar(255) DEFAULT NULL,
  KEY `FKgbjoox80o4yvw8k1e0ud0bnhm` (`diver_profile_id`),
  CONSTRAINT `FKgbjoox80o4yvw8k1e0ud0bnhm` FOREIGN KEY (`diver_profile_id`) REFERENCES `diver_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `diver_profiles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `certification_level` varchar(255) DEFAULT NULL,
  `completed_projects` int(11) NOT NULL,
  `equipment` varchar(500) DEFAULT NULL,
  `experience_years` int(11) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs31m3e52fy6r8h9gd9u62588i` (`user_id`),
  CONSTRAINT `FKsajuy4v928jkxmquw8seuwjsr` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `info_attachments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content_type` varchar(100) NOT NULL,
  `original_name` varchar(150) NOT NULL,
  `size_bytes` bigint(20) NOT NULL,
  `stored_name` varchar(80) NOT NULL,
  `request_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlo4y1fmuayl0fuh310foi6yh7` (`stored_name`),
  KEY `FKmpgep34401dpe9nxr6eup2287` (`request_id`),
  CONSTRAINT `FKmpgep34401dpe9nxr6eup2287` FOREIGN KEY (`request_id`) REFERENCES `info_requests` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `info_requests` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `message` varchar(1000) NOT NULL,
  `responded_at` datetime(6) DEFAULT NULL,
  `response_text` varchar(2000) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `report_id` bigint(20) NOT NULL,
  `requested_by_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKisjfwqpvhmameoscx8q53bhuj` (`report_id`),
  KEY `FKjl14hb5e3agy64pae7ev2etay` (`requested_by_id`),
  CONSTRAINT `FKisjfwqpvhmameoscx8q53bhuj` FOREIGN KEY (`report_id`) REFERENCES `pollution_reports` (`id`),
  CONSTRAINT `FKjl14hb5e3agy64pae7ev2etay` FOREIGN KEY (`requested_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `opportunities` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(2000) NOT NULL,
  `open` bit(1) NOT NULL,
  `paid` bit(1) NOT NULL,
  `region` varchar(255) NOT NULL,
  `required_certification` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `organization_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsq6v38rwf9c8jir5ajh165cik` (`organization_id`),
  CONSTRAINT `FKsq6v38rwf9c8jir5ajh165cik` FOREIGN KEY (`organization_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `opportunity_applications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `message` varchar(1000) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `diver_id` bigint(20) NOT NULL,
  `opportunity_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5b42oiq067u9qtqfbovsu7kpp` (`opportunity_id`,`diver_id`),
  KEY `FK4x1ky0kc5oxdj1m4kt8iwvt14` (`diver_id`),
  CONSTRAINT `FK4x1ky0kc5oxdj1m4kt8iwvt14` FOREIGN KEY (`diver_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKh0rv8b1trflhtx6ojaejwbx0b` FOREIGN KEY (`opportunity_id`) REFERENCES `opportunities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `password_reset_tokens` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKajre85ybxavf1tt4omkrs5p6g` (`token_hash`),
  KEY `FKk3ndxg5xp6v7wd4gjyusp15gq` (`user_id`),
  CONSTRAINT `FKk3ndxg5xp6v7wd4gjyusp15gq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pollution_reports` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `admin_decision` varchar(255) NOT NULL,
  `admin_reviewed_at` datetime(6) DEFAULT NULL,
  `alert_radius_km` double NOT NULL,
  `authority_comment` varchar(1000) DEFAULT NULL,
  `authority_decision` varchar(255) DEFAULT NULL,
  `confirm_votes` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `decided_at` datetime(6) DEFAULT NULL,
  `description` varchar(2000) NOT NULL,
  `dispute_votes` int(11) NOT NULL,
  `district` varchar(50) DEFAULT NULL,
  `escalated_at` datetime(6) DEFAULT NULL,
  `latitude` double NOT NULL,
  `location_name` varchar(255) NOT NULL,
  `longitude` double NOT NULL,
  `moderation_comment` varchar(1000) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `reference` varchar(255) NOT NULL,
  `reporter_will_lead` bit(1) NOT NULL DEFAULT b'1',
  `severity` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `trust_percentage` int(11) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `authority_officer_id` bigint(20) DEFAULT NULL,
  `reporter_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKegst8jtsmoh2g1oft8q54tpyk` (`reference`),
  KEY `FK6fnsk3rr32u7e50187ifffbhk` (`authority_officer_id`),
  KEY `FKpb2fymox1xe1fa4mv9svv28xb` (`reporter_id`),
  CONSTRAINT `FK6fnsk3rr32u7e50187ifffbhk` FOREIGN KEY (`authority_officer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpb2fymox1xe1fa4mv9svv28xb` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=263 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_equipment` (
  `project_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `quantity` int(11) NOT NULL,
  `secured_quantity` int(11) NOT NULL DEFAULT 0,
  `position` int(11) NOT NULL,
  PRIMARY KEY (`project_id`,`position`),
  CONSTRAINT `FK487aopc62km0ensxjjwsh2u13` FOREIGN KEY (`project_id`) REFERENCES `cleanup_projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_participants` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `contribution_mark` int(11) DEFAULT NULL,
  `joined_at` datetime(6) NOT NULL,
  `participant_role` varchar(255) NOT NULL,
  `project_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnfn4xoa7vx8t5bp16scgfwawh` (`project_id`,`user_id`),
  KEY `FKso729fy6p33s1cewrd5qw8u2v` (`user_id`),
  CONSTRAINT `FKji16xp3imgmii7erky2pw02fy` FOREIGN KEY (`project_id`) REFERENCES `cleanup_projects` (`id`),
  CONSTRAINT `FKso729fy6p33s1cewrd5qw8u2v` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=234 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_update_comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `body` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `author_id` bigint(20) NOT NULL,
  `update_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1bbc8s1548wcd3qsrsi4jd3dx` (`author_id`),
  KEY `FKikmeihk2ia55wbi045lnmkrct` (`update_id`),
  CONSTRAINT `FK1bbc8s1548wcd3qsrsi4jd3dx` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKikmeihk2ia55wbi045lnmkrct` FOREIGN KEY (`update_id`) REFERENCES `project_updates` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_update_likes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `update_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK66chuas99qfd9mxfbatntbvho` (`update_id`,`user_id`),
  KEY `FKn7eiq57c1yey4nha8km6mg27e` (`user_id`),
  CONSTRAINT `FK3of0vdugahcihtukso1rqplqv` FOREIGN KEY (`update_id`) REFERENCES `project_updates` (`id`),
  CONSTRAINT `FKn7eiq57c1yey4nha8km6mg27e` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_updates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `completion_percentage` int(11) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `image_url` varchar(1000) DEFAULT NULL,
  `note` varchar(1000) NOT NULL,
  `stage` varchar(255) NOT NULL,
  `author_id` bigint(20) DEFAULT NULL,
  `project_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKd4pa7lu4xunndqtalc27egr7e` (`author_id`),
  KEY `FK5wvwodpm0xv1eeonpwcbkudc5` (`project_id`),
  CONSTRAINT `FK5wvwodpm0xv1eeonpwcbkudc5` FOREIGN KEY (`project_id`) REFERENCES `cleanup_projects` (`id`),
  CONSTRAINT `FKd4pa7lu4xunndqtalc27egr7e` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=164 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `body` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `official` bit(1) NOT NULL,
  `author_id` bigint(20) NOT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  `report_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi8p0d6l24jm0hokvim8wspptq` (`author_id`),
  KEY `FKm9ckkf0napryt0kh8k5b0rp59` (`parent_id`),
  KEY `FK1sbfe31jw54dkvitrbryuiq5b` (`report_id`),
  CONSTRAINT `FK1sbfe31jw54dkvitrbryuiq5b` FOREIGN KEY (`report_id`) REFERENCES `pollution_reports` (`id`),
  CONSTRAINT `FKi8p0d6l24jm0hokvim8wspptq` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKm9ckkf0napryt0kh8k5b0rp59` FOREIGN KEY (`parent_id`) REFERENCES `report_comments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_photos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `caption` varchar(255) DEFAULT NULL,
  `content_type` varchar(100) DEFAULT NULL,
  `stored_name` varchar(80) DEFAULT NULL,
  `url` varchar(1000) NOT NULL,
  `report_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6iut7wlrvo8a4yk4wyo7aoiix` (`stored_name`),
  KEY `FKpqcn5oj5abmp6smgd3wek127f` (`report_id`),
  CONSTRAINT `FKpqcn5oj5abmp6smgd3wek127f` FOREIGN KEY (`report_id`) REFERENCES `pollution_reports` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=137 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_review_note` varchar(500) DEFAULT NULL,
  `account_status` varchar(255) NOT NULL,
  `available_for_alerts` bit(1) NOT NULL,
  `avatar_stored_name` varchar(80) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(50) DEFAULT NULL,
  `full_name` varchar(255) NOT NULL,
  `last_name` varchar(50) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `nic` varchar(12) DEFAULT NULL,
  `organization_name` varchar(255) DEFAULT NULL,
  `organization_type` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `role` varchar(255) NOT NULL,
  `suspended` bit(1) NOT NULL,
  `suspension_reason` varchar(500) DEFAULT NULL,
  `website_url` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `verification_votes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `confirmed` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `report_id` bigint(20) NOT NULL,
  `voter_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6o1333hr5g3lmqpubj4ksiloa` (`report_id`,`voter_id`),
  KEY `FKtnq8j38y7nw6g8pfd5vwk5qp4` (`voter_id`),
  CONSTRAINT `FK24pg5tjkci3uncdcp6s106s28` FOREIGN KEY (`report_id`) REFERENCES `pollution_reports` (`id`),
  CONSTRAINT `FKtnq8j38y7nw6g8pfd5vwk5qp4` FOREIGN KEY (`voter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=356 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

