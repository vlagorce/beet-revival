create sequence SEQ_BEHAVIOR_TRACKING_EVENT increment by 25;
create table BEHAVIOR_TRACKING_EVENT (
	EVENT_ID 		NUMERIC(10)		PRIMARY KEY,
	PARENT_EVENT_ID NUMERIC(10), 
	APPLICATION		VARCHAR2(32)	NOT NULL, 
	EVENT_TYPE			VARCHAR2(32)	NOT NULL, 
	EVENT_NAME			VARCHAR2(256)	NOT NULL, 
	EVENT_START			DATETIME		NOT NULL, 
	DURATION_NS		NUMERIC(10)		NOT NULL, 
	USER_ID			VARCHAR2(64), 
	SESSION_ID		VARCHAR2(64),
	ERROR			VARCHAR2(256), 
	EVENT_DATA		LONGVARCHAR
);