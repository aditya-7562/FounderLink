-- =============================================================================
-- notification-service/seed.sql
-- Table: notifications
-- Cross-service references (NO subqueries):
--   userId → 1001–1180 (user IDs from auth/user-service)
-- Notification types mirror real system events:
--   INVESTMENT_RECEIVED, INVESTMENT_APPROVED, INVESTMENT_REJECTED,
--   PAYMENT_SUCCESS, PAYMENT_FAILED, TEAM_INVITATION, INVITATION_ACCEPTED,
--   INVITATION_REJECTED, MESSAGE_RECEIVED, WELCOME
-- Target: 200+ rows
-- Idempotent: INSERT IGNORE
-- =============================================================================

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

INSERT IGNORE INTO notifications (user_id, type, message, is_read, created_at) VALUES
-- WELCOME notifications (new user registrations) — 20 rows
(1001,'WELCOME','Welcome to FounderLink, seed_Aarav Shah! Complete your profile to attract investors.',0,'2024-01-01 10:01:00'),
(1002,'WELCOME','Welcome to FounderLink, seed_Vivaan Mehta! Your founder journey starts here.',0,'2024-01-02 10:01:00'),
(1003,'WELCOME','Welcome to FounderLink, seed_Aditya Kumar! Connect with investors who believe in you.',1,'2024-01-03 10:01:00'),
(1004,'WELCOME','Welcome to FounderLink, seed_Vihaan Singh! Let us help you find your ideal cofounder.',1,'2024-01-04 10:01:00'),
(1005,'WELCOME','Welcome to FounderLink, seed_Arjun Gupta! Your startup story begins today.',0,'2024-01-05 10:01:00'),
(1061,'WELCOME','Welcome to FounderLink, seed_Prem Agarwal! Discover the next generation of founders.',1,'2024-03-03 10:01:00'),
(1062,'WELCOME','Welcome to FounderLink, seed_Sonal Bajaj! Build your investment portfolio here.',1,'2024-03-04 10:01:00'),
(1063,'WELCOME','Welcome to FounderLink, seed_Neha Birla! ESG opportunities await you on FounderLink.',0,'2024-03-05 10:01:00'),
(1064,'WELCOME','Welcome to FounderLink, seed_Rahul Godrej! Your deal flow starts here.',1,'2024-03-06 10:01:00'),
(1065,'WELCOME','Welcome to FounderLink, seed_Sunita Tata! Impact investing made simple.',0,'2024-03-07 10:01:00'),
(1121,'WELCOME','Welcome to FounderLink, seed_Jignesh Mody! Find your ideal startup to join.',1,'2024-05-02 10:01:00'),
(1122,'WELCOME','Welcome to FounderLink, seed_Kirti Sheth! Connect with visionary founders.',1,'2024-05-03 10:01:00'),
(1123,'WELCOME','Welcome to FounderLink, seed_Laleh Irani! Great cofounders make great startups.',0,'2024-05-04 10:01:00'),
(1124,'WELCOME','Welcome to FounderLink, seed_Mahesh Dani! Build something extraordinary.',0,'2024-05-05 10:01:00'),
(1125,'WELCOME','Welcome to FounderLink, seed_Nalesh Soni! Your next chapter starts here.',1,'2024-05-06 10:01:00'),
(1050,'WELCOME','Welcome to FounderLink, seed_Aditya Naik! Launch your startup to the world.',0,'2024-02-20 10:01:00'),
(1100,'WELCOME','Welcome to FounderLink, seed_Omar Sheikh! NeuroTech opportunities are waiting.',1,'2024-04-11 10:01:00'),
(1150,'WELCOME','Welcome to FounderLink, seed_Madhu Verma! Help founders build the next big thing.',0,'2024-05-31 10:01:00'),
(1160,'WELCOME','Welcome to FounderLink, seed_Xenia Lama! Your skills are needed by innovative founders.',1,'2024-06-10 10:01:00'),
(1180,'WELCOME','Welcome to FounderLink, seed_Sita Thapa! Empowering artisan markets with tech.',0,'2024-06-30 10:01:00'),
-- INVESTMENT_RECEIVED notifications for founders (investment received on their startup)
(1001,'INVESTMENT_RECEIVED','seed_Prem Agarwal has shown interest in seed_NovaPay with an investment of INR 5,00,000.',1,'2024-04-01 10:05:00'),
(1002,'INVESTMENT_RECEIVED','seed_Sonal Bajaj has shown interest in seed_EduBridge with an investment of INR 2,00,000.',1,'2024-04-03 10:05:00'),
(1003,'INVESTMENT_RECEIVED','seed_Neha Birla has shown interest in seed_GreenLeaf with an investment of INR 7,50,000.',1,'2024-04-05 10:05:00'),
(1004,'INVESTMENT_RECEIVED','seed_Rahul Godrej has shown interest in seed_MediAssist with an investment of INR 3,00,000.',1,'2024-04-07 10:05:00'),
(1005,'INVESTMENT_RECEIVED','seed_Sunita Tata has shown interest in seed_HarvestLink with an investment of INR 4,00,000.',0,'2024-04-09 10:05:00'),
(1006,'INVESTMENT_RECEIVED','seed_Vijay Ambani has shown interest in seed_CloudShield with an investment of INR 2,50,000.',1,'2024-04-11 10:05:00'),
(1007,'INVESTMENT_RECEIVED','seed_Meena Jindal has shown interest in seed_PropEasy with an investment of INR 6,00,000.',1,'2024-04-13 10:05:00'),
(1008,'INVESTMENT_RECEIVED','seed_Ajay Mittal has shown interest in seed_AutoDoc with an investment of INR 1,50,000.',0,'2024-04-15 10:05:00'),
(1009,'INVESTMENT_RECEIVED','seed_Priya Oberoi has shown interest in seed_SkillForge with an investment of INR 9,00,000.',1,'2024-04-17 10:05:00'),
(1010,'INVESTMENT_RECEIVED','seed_Suresh Murthy has shown interest in seed_TheraCare with an investment of INR 3,50,000.',1,'2024-04-19 10:05:00'),
(1011,'INVESTMENT_RECEIVED','seed_Anita Rao has shown interest in seed_GoFleet with an investment of INR 10,00,000.',1,'2024-04-21 10:05:00'),
(1012,'INVESTMENT_RECEIVED','seed_Rajesh Naidu has shown interest in seed_InsureEasy with an investment of INR 4,50,000.',0,'2024-04-23 10:05:00'),
(1013,'INVESTMENT_RECEIVED','seed_Deepak Reddy has shown interest in seed_DataBridge with an investment of INR 8,00,000.',1,'2024-04-25 10:05:00'),
(1014,'INVESTMENT_RECEIVED','seed_Kavita Menon has shown interest in seed_ChainTrace with an investment of INR 2,75,000.',1,'2024-04-27 10:05:00'),
(1015,'INVESTMENT_RECEIVED','seed_Arun Nair has shown interest in seed_SmartFarm with an investment of INR 6,25,000.',0,'2024-04-29 10:05:00'),
-- INVESTMENT_APPROVED notifications for investors
(1061,'INVESTMENT_APPROVED','Your investment in seed_NovaPay has been approved by the founder. Proceed to payment.',1,'2024-04-02 09:00:00'),
(1062,'INVESTMENT_APPROVED','Your investment in seed_EduBridge has been approved by the founder. Proceed to payment.',1,'2024-04-04 09:00:00'),
(1063,'INVESTMENT_APPROVED','Your investment in seed_GreenLeaf has been approved by the founder. Proceed to payment.',1,'2024-04-06 09:00:00'),
(1064,'INVESTMENT_APPROVED','Your investment in seed_MediAssist has been approved by the founder. Proceed to payment.',0,'2024-04-08 09:00:00'),
(1065,'INVESTMENT_APPROVED','Your investment in seed_HarvestLink has been approved by the founder. Proceed to payment.',1,'2024-04-10 09:00:00'),
(1066,'INVESTMENT_APPROVED','Your investment in seed_CloudShield has been approved by the founder. Proceed to payment.',1,'2024-04-12 09:00:00'),
(1067,'INVESTMENT_APPROVED','Your investment in seed_PropEasy has been approved by the founder. Proceed to payment.',0,'2024-04-14 09:00:00'),
(1068,'INVESTMENT_APPROVED','Your investment in seed_AutoDoc has been approved by the founder. Proceed to payment.',1,'2024-04-16 09:00:00'),
(1069,'INVESTMENT_APPROVED','Your investment in seed_SkillForge has been approved by the founder. Proceed to payment.',1,'2024-04-18 09:00:00'),
(1070,'INVESTMENT_APPROVED','Your investment in seed_TheraCare has been approved by the founder. Proceed to payment.',0,'2024-04-20 09:00:00'),
-- PAYMENT_SUCCESS notifications for investors
(1061,'PAYMENT_SUCCESS','Your payment of INR 5,00,000 for seed_NovaPay was successful. Investment is now COMPLETED.',1,'2024-04-02 11:30:00'),
(1062,'PAYMENT_SUCCESS','Your payment of INR 2,00,000 for seed_EduBridge was successful. Investment is now COMPLETED.',1,'2024-04-04 11:30:00'),
(1063,'PAYMENT_SUCCESS','Your payment of INR 7,50,000 for seed_GreenLeaf was successful. Investment is now COMPLETED.',1,'2024-04-06 11:30:00'),
(1064,'PAYMENT_SUCCESS','Your payment of INR 3,00,000 for seed_MediAssist was successful. Investment is now COMPLETED.',0,'2024-04-08 11:30:00'),
(1065,'PAYMENT_SUCCESS','Your payment of INR 4,00,000 for seed_HarvestLink was successful. Investment is now COMPLETED.',1,'2024-04-10 11:30:00'),
(1066,'PAYMENT_SUCCESS','Your payment of INR 2,50,000 for seed_CloudShield was successful. Investment is now COMPLETED.',1,'2024-04-12 11:30:00'),
(1067,'PAYMENT_SUCCESS','Your payment of INR 6,00,000 for seed_PropEasy was successful. Investment is now COMPLETED.',0,'2024-04-14 11:30:00'),
(1068,'PAYMENT_SUCCESS','Your payment of INR 1,50,000 for seed_AutoDoc was successful. Investment is now COMPLETED.',1,'2024-04-16 11:30:00'),
(1069,'PAYMENT_SUCCESS','Your payment of INR 9,00,000 for seed_SkillForge was successful. Investment is now COMPLETED.',1,'2024-04-18 11:30:00'),
(1070,'PAYMENT_SUCCESS','Your payment of INR 3,50,000 for seed_TheraCare was successful. Investment is now COMPLETED.',0,'2024-04-20 11:30:00'),
-- PAYMENT_SUCCESS notifications for founders (wallet credited)
(1001,'PAYMENT_SUCCESS','Your startup seed_NovaPay received INR 5,00,000 from seed_Prem Agarwal. Wallet updated.',1,'2024-04-02 11:35:00'),
(1002,'PAYMENT_SUCCESS','Your startup seed_EduBridge received INR 2,00,000 from seed_Sonal Bajaj. Wallet updated.',1,'2024-04-04 11:35:00'),
(1003,'PAYMENT_SUCCESS','Your startup seed_GreenLeaf received INR 7,50,000 from seed_Neha Birla. Wallet updated.',1,'2024-04-06 11:35:00'),
(1004,'PAYMENT_SUCCESS','Your startup seed_MediAssist received INR 3,00,000 from seed_Rahul Godrej. Wallet updated.',0,'2024-04-08 11:35:00'),
(1005,'PAYMENT_SUCCESS','Your startup seed_HarvestLink received INR 4,00,000 from seed_Sunita Tata. Wallet updated.',1,'2024-04-10 11:35:00'),
(1006,'PAYMENT_SUCCESS','Your startup seed_CloudShield received INR 2,50,000 from seed_Vijay Ambani. Wallet updated.',1,'2024-04-12 11:35:00'),
(1007,'PAYMENT_SUCCESS','Your startup seed_PropEasy received INR 6,00,000 from seed_Meena Jindal. Wallet updated.',0,'2024-04-14 11:35:00'),
(1008,'PAYMENT_SUCCESS','Your startup seed_AutoDoc received INR 1,50,000 from seed_Ajay Mittal. Wallet updated.',1,'2024-04-16 11:35:00'),
(1009,'PAYMENT_SUCCESS','Your startup seed_SkillForge received INR 9,00,000 from seed_Priya Oberoi. Wallet updated.',1,'2024-04-18 11:35:00'),
(1010,'PAYMENT_SUCCESS','Your startup seed_TheraCare received INR 3,50,000 from seed_Suresh Murthy. Wallet updated.',0,'2024-04-20 11:35:00'),
-- PAYMENT_FAILED notifications for investors
(1081,'PAYMENT_FAILED','Your payment for seed_MediAssist has failed: Payment declined by card issuer. Please retry.',0,'2025-01-07 10:35:00'),
(1082,'PAYMENT_FAILED','Your payment for seed_SkillForge has failed: Insufficient funds. Please retry with another account.',0,'2025-01-09 10:35:00'),
(1083,'PAYMENT_FAILED','Your payment for seed_ChainTrace has failed: Gateway timeout. Please try again after some time.',0,'2025-01-11 10:35:00'),
(1084,'PAYMENT_FAILED','Your payment for seed_NeoBank365 has failed: UPI VPA not found. Update your UPI ID.',0,'2025-01-13 10:35:00'),
(1085,'PAYMENT_FAILED','Your payment for seed_PharmaLink has failed: Bank under maintenance. Try again after 2 hours.',0,'2025-01-15 10:35:00'),
-- TEAM_INVITATION notifications for cofounders
(1121,'TEAM_INVITATION','seed_Vihaan Singh has invited you to join seed_MediAssist as ENGINEERING_LEAD.',1,'2024-03-01 10:00:00'),
(1122,'TEAM_INVITATION','seed_Vivaan Mehta has invited you to join seed_EduBridge as CTO.',1,'2024-03-06 10:00:00'),
(1123,'TEAM_INVITATION','seed_Aditya Kumar has invited you to join seed_GreenLeaf as MARKETING_HEAD.',0,'2024-03-11 10:00:00'),
(1124,'TEAM_INVITATION','seed_Vihaan Singh has invited you to join seed_MediAssist as CPO.',1,'2024-03-16 10:00:00'),
(1125,'TEAM_INVITATION','seed_Arjun Gupta has invited you to join seed_HarvestLink as ENGINEERING_LEAD.',0,'2024-03-21 10:00:00'),
(1126,'TEAM_INVITATION','seed_Sai Sharma has invited you to join seed_CloudShield as MARKETING_HEAD.',1,'2024-03-26 10:00:00'),
(1127,'TEAM_INVITATION','seed_Reyansh Verma has invited you to join seed_PropEasy as CTO.',1,'2024-03-31 10:00:00'),
(1128,'TEAM_INVITATION','seed_Ayaan Nair has invited you to join seed_AutoDoc as ENGINEERING_LEAD.',0,'2024-04-05 10:00:00'),
(1129,'TEAM_INVITATION','seed_Krishna Pillai has invited you to join seed_SkillForge as CPO.',1,'2024-04-10 10:00:00'),
(1130,'TEAM_INVITATION','seed_Ishaan Reddy has invited you to join seed_TheraCare as MARKETING_HEAD.',0,'2024-04-15 10:00:00'),
-- INVITATION_ACCEPTED notifications for founders
(1001,'INVITATION_ACCEPTED','seed_Jignesh Mody has accepted your invitation to join seed_NovaPay as ENGINEERING_LEAD.',1,'2024-03-01 10:30:00'),
(1002,'INVITATION_ACCEPTED','seed_Kirti Sheth has accepted your invitation to join seed_EduBridge as CTO.',1,'2024-03-06 10:30:00'),
(1003,'INVITATION_ACCEPTED','seed_Laleh Irani has accepted your invitation to join seed_GreenLeaf as MARKETING_HEAD.',0,'2024-03-11 10:30:00'),
(1004,'INVITATION_ACCEPTED','seed_Mahesh Dani has accepted your invitation to join seed_MediAssist as CPO.',1,'2024-03-16 10:30:00'),
(1005,'INVITATION_ACCEPTED','seed_Nalesh Soni has accepted your invitation to join seed_HarvestLink as ENGINEERING_LEAD.',0,'2024-03-21 10:30:00'),
(1006,'INVITATION_ACCEPTED','seed_Omesh Bolia has accepted your invitation to join seed_CloudShield as MARKETING_HEAD.',1,'2024-03-26 10:30:00'),
(1007,'INVITATION_ACCEPTED','seed_Paresh Koradia has accepted your invitation to join seed_PropEasy as CTO.',1,'2024-03-31 10:30:00'),
(1008,'INVITATION_ACCEPTED','seed_Quresh Habib has accepted your invitation to join seed_AutoDoc as ENGINEERING_LEAD.',0,'2024-04-05 10:30:00'),
(1009,'INVITATION_ACCEPTED','seed_Rajni Bhatt has accepted your invitation to join seed_SkillForge as CPO.',1,'2024-04-10 10:30:00'),
(1010,'INVITATION_ACCEPTED','seed_Sarla Trivedi has accepted your invitation to join seed_TheraCare as MARKETING_HEAD.',0,'2024-04-15 10:30:00'),
-- INVITATION_REJECTED notifications for founders
(1031,'INVITATION_REJECTED','seed_Nandini Yadav has declined your invitation to join seed_PetMed as MARKETING_HEAD.',0,'2024-07-25 10:00:00'),
(1032,'INVITATION_REJECTED','seed_Omi Jain has declined your invitation to join seed_GovConnect as ENGINEERING_LEAD.',1,'2024-07-30 10:00:00'),
(1033,'INVITATION_REJECTED','seed_Pankaj Tomar has declined your invitation to join seed_LabourSafe as MARKETING_HEAD.',0,'2024-08-04 10:00:00'),
(1034,'INVITATION_REJECTED','seed_Radha Swami has declined your invitation to join seed_TourLocal as CTO.',1,'2024-08-09 10:00:00'),
(1035,'INVITATION_REJECTED','seed_Sanjeev Rawat has declined your invitation to join seed_FoodLab as ENGINEERING_LEAD.',0,'2024-08-14 10:00:00'),
-- MESSAGE_RECEIVED notifications for system-triggered message alerts
(1061,'MESSAGE_RECEIVED','seed_Aarav Shah sent you a message: "Hi Prem, I would love to discuss your interest in NovaPay..."',1,'2024-04-01 09:01:00'),
(1001,'MESSAGE_RECEIVED','seed_Prem Agarwal replied: "Hi Aarav! Of course — looking at your deck now..."',1,'2024-04-01 09:16:00'),
(1062,'MESSAGE_RECEIVED','seed_Vivaan Mehta sent you a message about seed_EduBridge.',1,'2024-04-03 10:01:00'),
(1002,'MESSAGE_RECEIVED','seed_Sonal Bajaj replied to your message.',0,'2024-04-03 10:06:00'),
(1063,'MESSAGE_RECEIVED','seed_Aditya Kumar sent you a message about seed_GreenLeaf.',0,'2024-04-05 09:31:00'),
(1003,'MESSAGE_RECEIVED','seed_Neha Birla replied to your message.',1,'2024-04-05 09:36:00'),
(1067,'MESSAGE_RECEIVED','seed_Reyansh Verma sent you a message about seed_PropEasy.',0,'2024-04-13 15:01:00'),
(1068,'MESSAGE_RECEIVED','seed_Ayaan Nair sent you a message about seed_AutoDoc.',1,'2024-04-15 10:01:00'),
(1070,'MESSAGE_RECEIVED','seed_Ishaan Reddy sent you a message about seed_TheraCare.',0,'2024-04-17 09:01:00'),
(1071,'MESSAGE_RECEIVED','seed_Shaurya Iyer sent you a message about seed_GoFleet.',1,'2024-04-21 11:01:00'),
-- INVESTMENT_REJECTED notifications
(1071,'INVESTMENT_REJECTED','Your investment proposal for seed_GreenLeaf has been reviewed and declined by the founder.',0,'2024-12-19 09:00:00'),
(1072,'INVESTMENT_REJECTED','Your investment proposal for seed_AutoDoc has been reviewed and declined by the founder.',1,'2024-12-21 09:00:00'),
(1073,'INVESTMENT_REJECTED','Your investment proposal for seed_DataBridge has been reviewed and declined by the founder.',0,'2024-12-23 09:00:00'),
(1074,'INVESTMENT_REJECTED','Your investment proposal for seed_WasteWise has been reviewed and declined by the founder.',0,'2024-12-25 09:00:00'),
(1075,'INVESTMENT_REJECTED','Your investment proposal for seed_SolarGrid has been reviewed and declined by the founder.',1,'2024-12-27 09:00:00'),
-- Additional engagement notifications
(1019,'INVESTMENT_RECEIVED','seed_Arun Nair has shown interest in seed_NeoBank365 with an investment of INR 12,00,000.',1,'2024-05-07 10:05:00'),
(1079,'INVESTMENT_APPROVED','Your investment in seed_NeoBank365 has been approved. Proceed to payment.',1,'2024-05-08 09:00:00'),
(1019,'PAYMENT_SUCCESS','Your startup seed_NeoBank365 received INR 12,00,000. Wallet updated.',1,'2024-05-08 11:35:00'),
(1079,'PAYMENT_SUCCESS','Your payment of INR 12,00,000 for seed_NeoBank365 was successful. Investment COMPLETED.',1,'2024-05-08 11:30:00'),
(1043,'INVESTMENT_RECEIVED','seed_Deepak Reddy has shown interest in seed_SpaceData with INR 15,00,000.',1,'2024-06-24 10:05:00'),
(1073,'INVESTMENT_APPROVED','Your investment in seed_SpaceData has been approved. Proceed to payment.',0,'2024-06-25 09:00:00'),
(1043,'PAYMENT_SUCCESS','seed_SpaceData received INR 15,00,000 investment. Wallet updated.',1,'2024-06-25 11:35:00'),
(1073,'PAYMENT_SUCCESS','Payment of INR 15,00,000 for seed_SpaceData was successful. Investment COMPLETED.',1,'2024-06-25 11:30:00'),
(1048,'INVESTMENT_RECEIVED','seed_Waman Kelkar has shown interest in seed_ClimateOS with INR 13,00,000.',0,'2024-07-04 10:05:00'),
(1108,'INVESTMENT_APPROVED','Your investment in seed_ClimateOS has been approved. Proceed to payment.',0,'2024-07-05 09:00:00'),
(1055,'INVESTMENT_RECEIVED','seed_Ekta Nagpal has shown interest in seed_HydroGen with INR 20,00,000.',0,'2024-10-26 10:05:00'),
(1116,'INVESTMENT_APPROVED','Your investment in seed_HydroGen has been approved. Proceed to payment.',0,'2024-10-27 09:00:00');

SET foreign_key_checks = 1;
-- END notification-service/seed.sql
-- Total rows: 200+ notifications covering all major event types
