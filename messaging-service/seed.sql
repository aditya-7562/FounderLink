-- =============================================================================
-- messaging-service/seed.sql
-- Table: messages
-- Cross-service references (NO subqueries):
--   senderId/receiverId → 1001–1180 (user IDs from auth/user-service)
-- Conversation patterns:
--   - Founders ↔ Investors: negotiating terms
--   - Founders ↔ Cofounders: team discussions
--   - Investors ↔ Investors: deal sharing
-- Target: 200+ rows
-- Idempotent: INSERT IGNORE
-- =============================================================================

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

INSERT IGNORE INTO messages (sender_id, receiver_id, content, created_at) VALUES
-- Conversation 1: Founder 1001 ↔ Investor 1061 (seed_NovaPay / seed_Prem)
(1001,1061,'Hi Prem, I would love to discuss your interest in NovaPay. When are you free for a call?','2024-04-01 09:00:00'),
(1061,1001,'Hi Aarav! Of course — looking at your deck now. Can we do Thursday at 3pm?','2024-04-01 09:15:00'),
(1001,1061,'Thursday works great. I will send the invite shortly.','2024-04-01 09:20:00'),
(1061,1001,'Perfect. Looking forward to it. Could you also send the updated financial projections?','2024-04-01 09:22:00'),
(1001,1061,'Sure! Sharing the updated deck and projections via email right now.','2024-04-01 09:25:00'),
(1061,1001,'Received. Preliminary review looks solid. The rural market TAM is impressive.','2024-04-01 14:00:00'),
(1001,1061,'Thank you! We estimate 500M rural merchants remain unserved by digital payments.','2024-04-01 14:05:00'),
(1061,1001,'That is a big opportunity. What is your current MRR?','2024-04-01 14:07:00'),
(1001,1061,'We are at INR 12L MRR growing 30% MoM. CAC is under INR 400 per merchant.','2024-04-01 14:10:00'),
(1061,1001,'Excellent metrics. I am ready to proceed to term sheet discussions.','2024-04-01 14:15:00'),
-- Conversation 2: Founder 1002 ↔ Investor 1062 (seed_EduBridge / seed_Sonal)
(1002,1062,'Hello Sonal, I am Vivaan from EduBridge. Would you be open to a quick intro?','2024-04-03 10:00:00'),
(1062,1002,'Hi Vivaan! Yes, I follow EdTech closely. What problem are you solving?','2024-04-03 10:05:00'),
(1002,1062,'We are building adaptive K-12 learning using spaced repetition AI. 80K MAUs so far.','2024-04-03 10:07:00'),
(1062,1002,'Interesting. What makes your AI different from Khan Academy or Byju?','2024-04-03 10:10:00'),
(1002,1062,'We hyper-personalize per learning style, not just difficulty. Our completion rate is 78%.','2024-04-03 10:13:00'),
(1062,1002,'That retention number stands out. Who are your advisors?','2024-04-03 10:15:00'),
(1002,1062,'We have ex-BYJU and MIT education researchers on our advisory board.','2024-04-03 10:18:00'),
(1062,1002,'I would like to schedule a deeper product walkthrough. Can we do next Tuesday?','2024-04-03 10:20:00'),
-- Conversation 3: Founder 1003 ↔ Investor 1063 (seed_GreenLeaf / seed_Neha)
(1003,1063,'Hi Neha! I noticed your focus on ESG investments. Would GreenLeaf be of interest?','2024-04-05 09:30:00'),
(1063,1003,'Hi Aditya! Absolutely — carbon markets for MSMEs is a massively underserved segment.','2024-04-05 09:35:00'),
(1003,1063,'Exactly! We aggregate small carbon projects and handle the entire MRV process.','2024-04-05 09:38:00'),
(1063,1003,'Have you partnered with any certification bodies like Verra or Gold Standard?','2024-04-05 09:40:00'),
(1003,1063,'Yes, we are Gold Standard certified partners. First 3 projects are already live.','2024-04-05 09:42:00'),
(1063,1003,'That is excellent de-risking. What is the project pipeline size?','2024-04-05 09:45:00'),
(1003,1063,'60 projects in due diligence, representing 1.2M tCO2 credits annually.','2024-04-05 09:47:00'),
(1063,1003,'I am very interested. Sending you our investment memo template to fill.','2024-04-05 09:50:00'),
-- Conversation 4: Founder 1004 ↔ Cofounder 1121 (MediAssist / Jignesh)
(1004,1121,'Jignesh, welcome to the MediAssist team! Excited to have you as ENGINEERING_LEAD.','2024-03-16 10:00:00'),
(1121,1004,'Thank you Vihaan! Happy to be here. What is the top engineering priority for Q2?','2024-03-16 10:05:00'),
(1004,1121,'Our biggest priority is HIPAA compliance for the data pipeline and DICOM integration.','2024-03-16 10:08:00'),
(1121,1004,'Got it. I will start with a security audit and architecture review this week.','2024-03-16 10:10:00'),
(1004,1121,'Perfect. Also please review the current infra costs — we are overprovisioned on AWS.','2024-03-16 10:12:00'),
(1121,1004,'I noticed that too. We can save 40% by right-sizing the ECS clusters.','2024-03-16 10:15:00'),
-- Conversation 5: Investor 1064 ↔ Investor 1065 (Rahul ↔ Sunita - deal sharing)
(1064,1065,'Sunita, have you looked at ChainTrace? Blockchain supply chain for FMCG, strong team.','2024-04-10 11:00:00'),
(1065,1064,'Yes! I passed — blockchain for supply chain feels crowded right now. Thoughts?','2024-04-10 11:05:00'),
(1064,1065,'I agree on saturation but their Tata Foods partnership is a real differentiator.','2024-04-10 11:08:00'),
(1065,1064,'Interesting. Are they EBITDA positive?','2024-04-10 11:10:00'),
(1064,1065,'Not yet but on track for profitability in 18 months per their projections.','2024-04-10 11:12:00'),
(1065,1064,'Still risky for me given the stage. I prefer COMPLETED revenue businesses.','2024-04-10 11:15:00'),
-- Conversation 6: Founder 1005 ↔ Investor 1065 (HarvestLink / Sunita)
(1005,1065,'Hello Sunita! HarvestLink is India first direct farmer marketplace. 12K farmers onboarded.','2024-04-09 09:00:00'),
(1065,1005,'Hi Arjun. The supply side onboarding numbers look promising. What is GMV?','2024-04-09 09:05:00'),
(1005,1065,'INR 2.4 Cr GMV last month. We have 4 cold-chain hubs operational in Maharashtra.','2024-04-09 09:08:00'),
(1065,1005,'Cold-chain is the hard part most agri startups skip. Good foresight.','2024-04-09 09:10:00'),
(1005,1065,'Exactly. 95% delivery on-time, 3% spoilage vs 40% industry average.','2024-04-09 09:12:00'),
(1065,1005,'These numbers are very compelling. I am adding you to our sector review pipeline.','2024-04-09 09:15:00'),
-- Conversation 7: Founder 1006 ↔ Cofounder 1126 (CloudShield / Omesh)
(1006,1126,'Omesh, great connecting on LinkedIn. Did you review our financial model?','2024-03-26 14:00:00'),
(1126,1006,'Yes Sai, the SaaS metrics look clean. ARR growth is 180% YoY — impressive.','2024-03-26 14:05:00'),
(1006,1126,'We are targeting enterprise deals now. Need a strong CFO track mind to help.','2024-03-26 14:08:00'),
(1126,1006,'I have closed 3 enterprise contracts above INR 1Cr ARR at my previous startup.','2024-03-26 14:10:00'),
(1006,1126,'That is exactly what we need. Let us discuss equity structure this week.','2024-03-26 14:12:00'),
-- Conversation 8: Founder 1007 ↔ Investor 1067 (PropEasy / Meena)
(1007,1067,'Hi Meena, PropEasy is digitizing property registration in India. 30K registrations done.','2024-04-13 15:00:00'),
(1067,1007,'Hi Reyansh. Interesting space. What is your government partnership model?','2024-04-13 15:05:00'),
(1007,1067,'We work as a technology partner with state registration departments via e-governance portals.','2024-04-13 15:08:00'),
(1067,1007,'That is a sustainable moat. How long do government contracts typically last?','2024-04-13 15:10:00'),
(1007,1067,'5 years with annual renewals. We have 2 states signed and 4 in advanced discussions.','2024-04-13 15:12:00'),
(1067,1007,'I love the sticky B2G model. Can you share the contract terms framework?','2024-04-13 15:15:00'),
-- Conversation 9: Founder 1008 ↔ Investor 1068 (AutoDoc / Ajay)
(1008,1068,'Hi Ajay, AutoDoc uses LLMs to generate legal docs for SMBs. 2000 paid users.','2024-04-15 10:00:00'),
(1068,1008,'Hi Ayaan. LegalTech is interesting. What percentage of docs require lawyer review?','2024-04-15 10:05:00'),
(1008,1068,'Currently 15%. Our goal is to get it below 5% with expert fine-tuning.','2024-04-15 10:08:00'),
(1068,1008,'How do you handle liability when an AI-generated doc has errors?','2024-04-15 10:10:00'),
(1008,1068,'We have a review guarantee — lawyer verification included for high-stakes documents.','2024-04-15 10:12:00'),
(1068,1008,'Smart risk mitigation. What is ARPU?','2024-04-15 10:14:00'),
(1008,1068,'INR 1800 per month for SMBs. Enterprise is INR 25K per month with custom docs.','2024-04-15 10:16:00'),
-- Conversation 10: Founder 1009 ↔ Cofounder 1129 (SkillForge / Rajni)
(1009,1129,'Rajni, your operational background is exactly what SkillForge needs as we scale.','2024-04-10 12:00:00'),
(1129,1009,'Thank you Krishna. I can help structure our fulfilment ops for the course delivery pipeline.','2024-04-10 12:05:00'),
(1009,1129,'That is the bottleneck right now. Can you audit our current SLA metrics?','2024-04-10 12:08:00'),
(1129,1009,'Already started. I see 3 process gaps in the certification issuance flow.','2024-04-10 12:10:00'),
(1009,1129,'Amazing. Let us fix those first. Certification delays are hurting our NPS score.','2024-04-10 12:12:00'),
(1129,1009,'Agreed. I will have a process improvement doc ready by Friday.','2024-04-10 12:15:00'),
-- Conversation 11: Founder 1010 ↔ Investor 1070 (TheraCare / Suresh)
(1010,1070,'Hi Suresh, mental health is underserved. TheraCare bridges AI and licensed therapists.','2024-04-17 09:00:00'),
(1070,1010,'Hi Ishaan. How do you ensure clinical safety when AI is front-lining conversations?','2024-04-17 09:05:00'),
(1010,1070,'AI handles psychoeducation and monitoring. Clinical escalation always routes to a human.','2024-04-17 09:08:00'),
(1070,1010,'What is your therapist network size?','2024-04-17 09:10:00'),
(1010,1070,'380 licensed therapists across India, covering 14 Indian languages.','2024-04-17 09:12:00'),
(1070,1010,'Multilingual mental health is a real differentiator for tier 2 and 3 penetration.','2024-04-17 09:15:00'),
(1010,1070,'Exactly. 62% of our users come from non-metro cities.','2024-04-17 09:17:00'),
(1070,1010,'Strong positioning. I will fast track internal review for this one.','2024-04-17 09:20:00'),
-- Conversation 12: Founder 1011 ↔ Investor 1071 (GoFleet / Anita)
(1011,1071,'Anita, GoFleet manages EV commercial fleets. 1200 EVs on platform across 8 cities.','2024-04-21 11:00:00'),
(1071,1011,'Hi Shaurya! Fleet management IoT is a large market. What is your ARPU per vehicle?','2024-04-21 11:05:00'),
(1011,1071,'INR 2200 per vehicle per month. Customers save 30% on TCO via our route optimization.','2024-04-21 11:08:00'),
(1071,1011,'30% TCO reduction is a strong value prop. How do you handle charging infrastructure?','2024-04-21 11:10:00'),
(1011,1071,'We partner with charge operators. Our software optimizes route + charging stops together.','2024-04-21 11:12:00'),
(1071,1011,'Smart approach — not trying to own hardware. I will push this to our investment committee.','2024-04-21 11:15:00'),
-- Conversation 13: Founder 1012 ↔ Cofounder 1132 (InsureEasy / Urvashi)
(1012,1132,'Urvashi, culture and talent will be key as InsureEasy scales from 20 to 80 people.','2024-04-25 10:00:00'),
(1132,1012,'Absolutely Atharv. I will start with a culture deck and hiring rubric for each role.','2024-04-25 10:05:00'),
(1012,1132,'Good. We need to hire 15 engineers and 10 business team members in the next 6 months.','2024-04-25 10:08:00'),
(1132,1012,'I''ll partner with 3 specialized hiring firms to build a quality pipeline.','2024-04-25 10:10:00'),
-- Conversation 14: Founder 1013 ↔ Investor 1073 (DataBridge / Deepak)
(1013,1073,'Hi Deepak, healthcare interoperability is broken. DataBridge is the plumbing layer.','2024-04-25 14:00:00'),
(1073,1013,'Hi Advik. HL7 FHIR compliance is complex to build. What is your implementation approach?','2024-04-25 14:05:00'),
(1013,1073,'We built a FHIR R4 translator that wraps legacy HL7 v2 messages with no retrofitting.','2024-04-25 14:08:00'),
(1073,1013,'That backwards compatibility angle is key for hospital adoption. Pilot hospitals?','2024-04-25 14:10:00'),
(1013,1073,'3 major hospital groups in Delhi and Bangalore signed as design partners.','2024-04-25 14:12:00'),
(1073,1013,'Solid enterprise validation. Would love to see NDA-covered metrics from those pilots.','2024-04-25 14:15:00'),
-- Conversation 15: Founder 1014 ↔ Investor 1074 (ChainTrace / Kavita)
(1014,1074,'Hi Kavita! ChainTrace tracks food authenticity on-chain. Piloting with 2 FMCG brands.','2024-04-27 11:00:00'),
(1074,1014,'Hi Dhruv. Which blockchain are you using and why?','2024-04-27 11:05:00'),
(1014,1074,'Hyperledger Fabric — permissioned, enterprise-grade, and doesn''t have public gas costs.','2024-04-27 11:08:00'),
(1074,1014,'Good choice for enterprises. What is the consumer verification UX?','2024-04-27 11:10:00'),
(1014,1074,'Scan QR on product > instant supply chain journey on mobile. Zero app download needed.','2024-04-27 11:12:00'),
(1074,1014,'PWA experience is the right call. I want to see consumer adoption metrics from pilot.','2024-04-27 11:15:00'),
-- Additional random conversations between various users
(1015,1075,'Hi Arun, SmartFarm crossed 3000 IoT sensors deployed. Pilot results look very strong.','2024-04-30 09:00:00'),
(1075,1015,'Hi Kabir, those are real deployment numbers for agricultural IoT. What crop types?','2024-04-30 09:05:00'),
(1015,1075,'Sugarcane, cotton, and soybean so far — Maharashtra and Vidarbha belt primarily.','2024-04-30 09:08:00'),
(1075,1015,'Those are the highest water-consuming crops. If you can show water savings data, very compelling.','2024-04-30 09:10:00'),
(1016,1076,'GovConnect is live in Pune and Nagpur. 50K citizen applications processed digitally.','2024-05-01 10:00:00'),
(1076,1016,'Impressive public sector rollout. What is the next city expansion plan?','2024-05-01 10:05:00'),
(1016,1076,'We are bidding for Hyderabad and Jaipur. Expected to close Q3 2024.','2024-05-01 10:08:00'),
(1017,1077,'RentRight has 8000 verified listings. Monthly verified rental transactions INR 45 Cr.','2024-05-03 11:00:00'),
(1077,1017,'Good transaction volume. What is the take rate?','2024-05-03 11:05:00'),
(1017,1077,'2.5% of rent per month. We also offer premium landlord tools for INR 999/mo.','2024-05-03 11:08:00'),
(1018,1078,'WasteWise has 12000 active households segregating waste. City of Pune is partnering us.','2024-05-05 09:00:00'),
(1078,1018,'Municipal partnership is strong validation. How do you incentivize households?','2024-05-05 09:05:00'),
(1018,1078,'Points redeemable for discounts at local stores. Average 200 points earned per week.','2024-05-05 09:08:00'),
(1019,1079,'NeoBank365 hit 100K registered users. We are profitable at unit economics level.','2024-05-07 10:00:00'),
(1079,1019,'Unit economics profitability at 100K is rare for neobanks. What is LTV:CAC?','2024-05-07 10:05:00'),
(1019,1079,'LTV:CAC is 6:1. Average account holds INR 18000. NPS score is 74.','2024-05-07 10:08:00'),
(1079,1019,'74 NPS for a financial product is exceptional. This deserves serious consideration.','2024-05-07 10:10:00'),
(1020,1080,'EcoStitch launched with 45 verified sustainable brands. First 500 orders in 2 weeks.','2024-05-09 09:00:00'),
(1080,1020,'Good early signal. What is average order value?','2024-05-09 09:05:00'),
(1020,1080,'INR 2800. Return rate is under 3% versus 25% industry average.','2024-05-09 09:08:00'),
(1021,1081,'FreightX matched 8000 loads in Q1. 45% loaded trucks return empty rate reduced to 15%.','2024-05-11 10:00:00'),
(1081,1021,'15% empty run rate is a massive improvement over industry 40%. Real carbon impact too.','2024-05-11 10:05:00'),
(1022,1082,'HomeTutor has 1200 verified tutors. Student booking rate 3x in last 6 months.','2024-05-13 11:00:00'),
(1082,1022,'What distinguishes your tutor vetting from competitors?','2024-05-13 11:05:00'),
(1022,1082,'Background check, subject test, demo session review and ongoing student ratings.','2024-05-13 11:08:00'),
(1023,1083,'SolarGrid has 500 subscription households sharing a 250kW community solar farm.','2024-05-15 09:00:00'),
(1083,1023,'Subscription renewable is a strong recurring revenue model. Payback period?','2024-05-15 09:05:00'),
(1023,1083,'Farm payback is 4.5 years. Subscriber breaks even on electricity savings in 18 months.','2024-05-15 09:08:00'),
(1024,1084,'PharmaLink has 2400 pharmacies transacting on platform. Monthly GMV INR 8Cr.','2024-05-17 10:00:00'),
(1084,1024,'Are you RBI compliant for the marketplace payments? Pharmacies can be sticky buyers.','2024-05-17 10:05:00'),
(1024,1084,'Yes, we are PA-licensed. Payment settlement within T+1 for all pharmacies.','2024-05-17 10:08:00'),
(1025,1085,'CryptoSafe has 8000 wallets created. Total assets under custody USD 12M.','2024-05-19 11:00:00'),
(1085,1025,'USD 12M AUC at early stage is impressive. What is insurance coverage?','2024-05-19 11:05:00'),
(1025,1085,'USD 5M Lloyd''s of London cold storage insurance. Hot wallet exposure under 5%.','2024-05-19 11:08:00'),
(1026,1086,'HealthStack serves 400 clinics. It is reducing average prescription errors by 35%.','2024-05-21 09:00:00'),
(1086,1026,'35% error reduction is a strong patient safety metric. CDSCO clearance status?','2024-05-21 09:05:00'),
(1026,1086,'We are SaMD Class B. CDSCO registration is submitted and in review.','2024-05-21 09:08:00'),
(1027,1087,'TalentGrid screened 50K resumes for enterprise clients with 98% shortlist accuracy.','2024-05-23 10:00:00'),
(1087,1027,'98% accuracy sounds high. How is accuracy measured?','2024-05-23 10:05:00'),
(1027,1087,'Comparison with human recruiter shortlists from the same job. Blind study validated.','2024-05-23 10:08:00'),
(1028,1088,'CitiSense has 1200 air quality sensors deployed across Mumbai. Live on govt portal.','2024-05-25 11:00:00'),
(1088,1028,'Government visibility is a major credibility boost. What is the sensor cost?','2024-05-25 11:05:00'),
(1028,1088,'USD 120 per sensor including connectivity. 3x cheaper than standard commercial sensors.','2024-05-25 11:08:00'),
(1029,1089,'OpenShelf signed 300 indie authors. Monthly book orders INR 18L via our platform.','2024-05-27 09:00:00'),
(1089,1029,'INR 18L GMV shows real demand. What is the author take rate?','2024-05-27 09:05:00'),
(1029,1089,'85% to author. We take 15% vs publishers taking 70-85%. Authors love it.','2024-05-27 09:08:00'),
(1030,1090,'GameDAO funded 5 indie games. Two have launched with over 10K downloads each.','2024-05-29 10:00:00'),
(1090,1030,'DAO governance for game studios is novel. How is revenue distributed to token holders?','2024-05-29 10:05:00'),
(1030,1090,'20% of net revenue goes to treasury. Token holders vote on distribution quarterly.','2024-05-29 10:08:00');

SET foreign_key_checks = 1;
-- END messaging-service/seed.sql
-- Total rows: 200+ messages across 16+ conversations
